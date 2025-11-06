'use client';

import MatchResultCard from '@/src/components/ui/MatchResultCard';
import type { Game } from '@/src/types/game';

interface GamesListProps {
  games: Game[];
  tournamentId: string; // MatchResultCard expects a string in your codebase
  editable: boolean;
  setsToWin: number;
  onInfoSaved: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  onTimeChanged: (gameId: string, newTime: string) => void;
  stage: string;
}

export default function GamesList({
  games,
  tournamentId,
  editable,
  setsToWin,
  onInfoSaved,
  onTimeChanged,
  stage,
}: GamesListProps) {
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
            />
          </div>
        );
      })}
    </div>
  );
}
