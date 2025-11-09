'use client';

import { useMemo } from 'react';
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import type { Game } from '@/src/types/game';

interface GamesListProps {
  games: Game[];
  tournamentId: string;
  editable: boolean;
  setsToWin: number;
  onInfoSaved: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  onTimeChanged: (gameId: string, newTime: string) => void;
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
  stage,
  isFirstRound = false,
}: GamesListProps) {
  // Créer une clé unique basée sur le stage et les IDs des matchs
  // key basée sur l'ensemble des IDs triés (indépendante de l'ordre)
  const gamesKey = `${stage}-${[...games.map(g => g.id)].sort().join('-')}`;

  // Liste d'IDs triée de manière déterministe (numérique si possible)
  // utilisée pour dériver un index stable et indépendant du ré-ordonnancement UI
  const sortedIds = useMemo(() => {
    const ids = [...games.map(g => g.id)];
    ids.sort((a, b) => {
      const na = Number(a);
      const nb = Number(b);
      if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb;
      return a.localeCompare(b);
    });
    return ids;
  }, [gamesKey]);

  const totalMatches = games.length;

  if (!games || games.length === 0) {
    return <p className="text-muted-foreground text-center">Aucun match à afficher</p>;
  }

  return (
    <div className="flex flex-col gap-4 w-full items-center mb-4">
      {games.map((game) => {
        const winnerIndex = game.finished
          ? game.winnerSide === 'TEAM_A'
            ? 0
            : game.winnerSide === 'TEAM_B'
              ? 1
              : undefined
          : undefined;

        // Récupérer l'index d'origine du match de façon déterministe via sortedIds
        const originalIndex = Math.max(0, sortedIds.indexOf(game.id));

        return (
          <div key={game.id} className="w-full max-w-xl flex justify-center">
            <MatchResultCard
              teamA={game.teamA}
              teamB={game.teamB}
              score={game.score}
              gameId={String(game.id)}
              tournamentId={tournamentId}
              editable={editable}
              court={game.court}
              scheduledTime={game.scheduledTime}
              onInfoSaved={onInfoSaved}
              onTimeChanged={onTimeChanged}
              winnerSide={winnerIndex}
              stage={stage}
              pool={(game as any).pool}
              setsToWin={setsToWin}
              finished={game.finished}
              matchIndex={originalIndex}
              totalMatches={totalMatches}
              isFirstRound={isFirstRound}
            />
          </div>
        );
      })}
    </div>
  );
}
