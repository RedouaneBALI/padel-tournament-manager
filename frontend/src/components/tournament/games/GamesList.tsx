'use client';

import React, { useMemo, useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import type { Game } from '@/src/types/game';
import { useRealtimeGames } from '@/src/hooks/useRealtimeGames';
import { exportGamesAsCSV } from '@/src/utils/gamesExport';
import { useExport } from '@/src/contexts/ExportContext';

function compareIds(a: string, b: string): number {
  const na = Number(a);
  const nb = Number(b);
  if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb;
  return a.localeCompare(b);
}

interface GamesListProps {
  games: Game[];
  tournamentId: string;
  editable: boolean;
  setsToWin: number;
  onInfoSaved: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  onTimeChanged: (gameId: string, newTime: string) => void;
  onGameUpdated?: (gameId: string, changes: { scheduledTime?: string; court?: string }) => void;
  stage: string;
  isFirstRound?: boolean;
}

export default function GamesList({
  games,
  tournamentId,
  editable,
  setsToWin,
  onInfoSaved,
  onTimeChanged,
  onGameUpdated,
  stage,
  isFirstRound = false,
}: GamesListProps) {
  const router = useRouter();
  // overrides locaux (score + winner) quand un match a été modifié (par MatchResultCard dispatch)
  const [overrides, setOverrides] = useState<Record<string, { score: any; winner: string | null }>>({});
  // État de tri local (time | court)
  const [sortMode, setSortMode] = useState<'time' | 'court'>('time');
  const [showOnlyInProgress, setShowOnlyInProgress] = useState(false);

  const handleGameClick = (gameId: string) => {
    const basePath = editable ? '/admin/tournament' : '/tournament';
    router.push(`${basePath}/${tournamentId}/games/${gameId}`);
  };

  // Écouter les mises à jour locales dispatchées par MatchResultCard
  useEffect(() => {
    const handler = (e: Event) => {
      try {
        const detail = (e as CustomEvent).detail;
        if (!detail || !detail.gameId) return;
        setOverrides(prev => ({
          ...prev,
          [String(detail.gameId)]: {
            score: detail.score,
            winner: detail.winner ?? null
          }
        }));
      } catch (err) {
        // ignore
      }
    };
    window.addEventListener('game-updated', handler as EventListener);
    return () => window.removeEventListener('game-updated', handler as EventListener);
  }, []);

  // Créer une clé unique basée sur le stage et les IDs des matchs
  // key basée sur l'ensemble des IDs triés (indépendante de l'ordre)
  const gamesKey = `${stage}-${[...games.map(g => g.id)].sort().join('-')}`;

  // Liste d'IDs triée de manière déterministe (numérique si possible)
  // utilisée pour dériver un index stable et indépendant du ré-ordonnancement UI
  const sortedIds = useMemo(() => {
    const ids = [...games.map(g => g.id)];
    ids.sort(compareIds);
    return ids;
  }, [gamesKey]);

  const totalMatches = games.length;

  // IDs des matchs non terminés pour les abonnements WebSocket
  const liveGameIds = useMemo(() => {
    return games.filter(g => !g.finished).map(g => String(g.id));
  }, [games]);

  // Real-time WebSocket updates for all non-finished games
  const handleRealtimeUpdate = useCallback((gameId: string, dto: { score: any; winner: string | null }) => {
    setOverrides(prev => ({
      ...prev,
      [gameId]: {
        score: dto.score,
        winner: dto.winner
      }
    }));
  }, []);

  useRealtimeGames({
    gameIds: liveGameIds,
    onScoreUpdate: handleRealtimeUpdate,
    enabled: liveGameIds.length > 0,
  });

  // Calculer s'il y a au moins un match en cours (live) - prend en compte les overrides
  const hasLiveMatches = useMemo(() => {
    return games.some(game => {
      const override = overrides[String(game.id)];
      const effectiveScore = override?.score ?? game.score;
      const isFinished = override?.winner ? true : game.finished;
      return !isFinished && (effectiveScore?.sets?.some((set: any) => set.teamAScore || set.teamBScore) || false);
    });
  }, [games, overrides]);

  // Liste triée en UI selon le mode sélectionné
  const orderedGames = useMemo(() => {
    if (!games || games.length === 0) return [] as Game[];

    // Helper pour comparer les horaires (mettre les matchs sans horaire à la fin)
    const compareTime = (a: Game, b: Game) => {
      if (!a.scheduledTime && !b.scheduledTime) return 0;
      if (!a.scheduledTime) return 1;
      if (!b.scheduledTime) return -1;
      return a.scheduledTime.localeCompare(b.scheduledTime);
    };

    let sortedGames: Game[];
    if (sortMode === 'time') {
      sortedGames = [...games].sort((a, b) => compareTime(a, b));
    } else {
      // sortMode === 'court' : trier par court (locale, numérique), puis par heure
      sortedGames = [...games].sort((a, b) => {
        const ca = (a.court || '').trim();
        const cb = (b.court || '').trim();

        if (!ca && !cb) return compareTime(a, b);
        if (!ca) return 1; // without court -> place after
        if (!cb) return -1;

        const courtCmp = ca.localeCompare(cb, 'fr', { numeric: true });
        if (courtCmp !== 0) return courtCmp;
        return compareTime(a, b);
      });
    }

    // Filtrer si nécessaire
    if (showOnlyInProgress) {
      sortedGames = sortedGames.filter(game => {
        const isInProgress = !game.finished && (game.score?.sets?.some(set => set.teamAScore || set.teamBScore) || false);
        return isInProgress;
      });
    }

    return sortedGames;
  }, [games, sortMode, showOnlyInProgress]);

  return (
    <div className="flex flex-col gap-4 w-full items-center mb-4">
      {/* Contrôle de tri (UI only) */}
      <div className="w-full max-w-xl flex justify-center">
        <div className="inline-flex items-center rounded-full bg-card p-1 space-x-1" role="tablist" aria-label="Mode de tri des matchs">
          <button
            type="button"
            role="tab"
            aria-pressed={sortMode === 'time'}
            title="Trier par heure"
            onClick={() => setSortMode('time')}
            className={`px-3 py-1 rounded-full text-sm transition-colors disabled:opacity-50 ${
              sortMode === 'time' ? 'bg-primary text-on-primary' : 'bg-transparent text-foreground hover:bg-primary/10'
            }`}
          >
            Par heure
          </button>
          <button
            type="button"
            role="tab"
            aria-pressed={sortMode === 'court'}
            title="Trier par court"
            onClick={() => setSortMode('court')}
            className={`px-3 py-1 rounded-full text-sm transition-colors disabled:opacity-50 ${
              sortMode === 'court' ? 'bg-primary text-on-primary' : 'bg-transparent text-foreground hover:bg-primary/10'
            }`}
          >
            Par court
          </button>
          {hasLiveMatches && (
            <>
              <button
                type="button"
                title={showOnlyInProgress ? "Afficher tous les matchs" : "Afficher uniquement les matchs en cours"}
                onClick={() => setShowOnlyInProgress(!showOnlyInProgress)}
                className={`w-6 h-6 rounded-full ml-2 transition-colors ${showOnlyInProgress ? 'bg-red-500' : 'border-2 border-red-500 bg-transparent'}`}
              ></button>
              <span className="text-sm text-muted-foreground ml-1 px-1">Live</span>
            </>
          )}
        </div>
      </div>

      {orderedGames.map((game) => {
        const override = overrides[String(game.id)];

        // Utiliser le winner de l'API si disponible dans les overrides, sinon utiliser game.winnerSide
        const winner = override?.winner !== undefined ? override.winner : (game.finished ? game.winnerSide : null);

        const winnerIndex = winner === 'TEAM_A' ? 0 : winner === 'TEAM_B' ? 1 : undefined;

        // Récupérer l'index d'origine du match de façon déterministe via sortedIds
        const originalIndex = Math.max(0, sortedIds.indexOf(game.id));

        return (
          <div key={game.id} className="w-full max-w-xl flex justify-center">
            <div
              onClick={() => handleGameClick(String(game.id))}
              className="cursor-pointer transition-transform hover:scale-[1.02] w-full flex justify-center"
            >
              <MatchResultCard
                teamA={game.teamA}
                teamB={game.teamB}
                score={override?.score ?? game.score}
                gameId={String(game.id)}
                tournamentId={tournamentId}
                editable={editable}
                court={game.court}
                scheduledTime={game.scheduledTime}
                onInfoSaved={onInfoSaved}
                onTimeChanged={onTimeChanged}
                onGameUpdated={onGameUpdated}
                winnerSide={winnerIndex}
                stage={stage}
                pool={(game as any).pool}
                setsToWin={setsToWin}
                finished={winner !== null}
                matchIndex={originalIndex}
                totalMatches={totalMatches}
                isFirstRound={isFirstRound}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}