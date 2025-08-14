'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { Loader2 } from 'lucide-react';
// Helper to get setsToWin for a round
function getSetsToWin(round: any) {
  const n = Number(round?.matchFormat?.numberOfSetsToWin);
  return Number.isFinite(n) && n > 0 ? n : 2;
}
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import RoundSelector from '@/src/components/round/RoundSelector';
import type { Round } from '@/src/types/round';
import type { Game } from '@/src/types/game';
import { fetchRounds } from '@/src/api/tournamentApi';

interface TournamentGamesTabProps {
  tournamentId: string;
  editable: boolean;
}

export default function TournamentGamesTab({ tournamentId, editable }: TournamentGamesTabProps) {
  const [rounds, setRounds] = useState<Round[]>([]);
  const [currentRoundIndex, setCurrentRoundIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const stageParam = searchParams?.get('stage') ?? null;

  // Fonction pour filtrer et trier les matchs d'un round par heure
  const getValidGamesSortedByTime = useCallback((games: Game[]) => {
    return games
      .filter(game =>
        (game.teamA !== null || game.teamB !== null) &&
        (game.teamA?.player1?.name !== 'BYE' &&
         game.teamA?.player2?.name !== 'BYE' &&
         game.teamB?.player1?.name !== 'BYE' &&
         game.teamB?.player2?.name !== 'BYE')
      )
      .sort((a, b) => {
        if (!a.scheduledTime && !b.scheduledTime) return 0;
        if (!a.scheduledTime) return 1;
        if (!b.scheduledTime) return -1;
        return a.scheduledTime.localeCompare(b.scheduledTime);
      });
  }, []);

  // Fonction pour mettre à jour l'heure d'un match localement
  const handleTimeChanged = useCallback((gameId: string, newTime: string) => {
    setRounds(currentRounds => {
      return currentRounds.map(round => {
        const gameExistsInRound = round.games.some(game => game.id === gameId);
        if (!gameExistsInRound) {
          return round;
        }

        return {
          ...round,
          games: round.games.map(game =>
            game.id === gameId
              ? { ...game, scheduledTime: newTime }
              : game
          ),
        };
      });
    });
  }, []);

  const handleInfoSaved = useCallback((result: { tournamentUpdated: boolean; winner: string | null }) => {
    if (result.tournamentUpdated) {
      fetchRounds(tournamentId)
        .then(newRounds => setRounds(newRounds))
        .catch(error => console.error("Erreur lors du rafraîchissement des rounds :", error));
    }
  }, [tournamentId]);

  useEffect(() => {
    async function loadRounds() {
      try {
        setIsLoading(true);
        const initialRounds = await fetchRounds(tournamentId);
        setRounds(initialRounds);
        if (initialRounds.length > 0) {
          const desiredStage = stageParam || initialRounds[0].stage;
          const idx = initialRounds.findIndex(r => r.stage === desiredStage);
          setCurrentRoundIndex(idx >= 0 ? idx : 0);
          if (!stageParam) {
            const sp = new URLSearchParams(searchParams?.toString?.() ?? '');
            sp.set('stage', desiredStage);
            router.replace(`${pathname}?${sp.toString()}`);
          }
        } else {
          setCurrentRoundIndex(0);
        }
      } catch (err) {
        console.error('Erreur lors du chargement des matchs :', err);
      } finally {
        setIsLoading(false);
      }
    }

    loadRounds();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId]);

  useEffect(() => {
    if (!rounds.length || !stageParam) return;
    const idx = rounds.findIndex(r => r.stage === stageParam);
    if (idx !== -1 && idx !== currentRoundIndex) {
      setCurrentRoundIndex(idx);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stageParam, rounds]);

  const handleStageChangeInUrl = useCallback((newStage: string) => {
    if (!newStage) return;
    const sp = new URLSearchParams(searchParams?.toString?.() ?? '');
    sp.set('stage', newStage);
    router.replace(`${pathname}?${sp.toString()}`);
    const idx = rounds.findIndex(r => r.stage === newStage);
    if (idx !== -1) setCurrentRoundIndex(idx);
  }, [router, pathname, searchParams, rounds]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    );
  }

  if (rounds.length === 0) {
    return <p className="text-muted">Aucun round défini pour le moment.</p>;
  }

  const currentRound = rounds[currentRoundIndex];
  const sortedGames = currentRound ? getValidGamesSortedByTime(currentRound.games) : [];

  return (
    <div className="flex flex-col items-center space-y-6">
      <RoundSelector
        rounds={rounds}
        currentIndex={currentRoundIndex}
        onChange={(idx) => {
          setCurrentRoundIndex(idx);
          const stage = rounds[idx]?.stage;
          if (stage) handleStageChangeInUrl(stage);
        }}
        onStageChange={handleStageChangeInUrl}
      />

      {sortedGames.length === 0 ? (
        <p className="text-muted">Aucun match trouvé pour ce round.</p>
      ) : (
        <div className="flex flex-col items-center space-y-4 w-full">
          {sortedGames.map(game => (
            <div key={game.id} className="w-full flex justify-center">
              <MatchResultCard
                teamA={game.teamA}
                teamB={game.teamB}
                score={game.score}
                gameId={game.id}
                tournamentId={tournamentId}
                editable={editable}
                court={game.court}
                scheduledTime={game.scheduledTime}
                onInfoSaved={handleInfoSaved}
                onTimeChanged={handleTimeChanged}
                winnerSide={
                  game.finished
                    ? game.winnerSide === 'TEAM_A'
                      ? 0
                      : game.winnerSide === 'TEAM_B'
                        ? 1
                        : undefined
                    : undefined
                }
                stage={currentRound.stage}
                pool={game.pool}
                setsToWin={getSetsToWin(currentRound)}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}