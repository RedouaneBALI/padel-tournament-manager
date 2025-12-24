import React, { useMemo, useState, useCallback } from 'react';
import { Game } from '@/src/types/game';
import { Tournament } from '@/src/types/tournament';
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import Link from 'next/link';
import { useRealtimeGames } from '@/src/hooks/useRealtimeGames';
import { TeamSide } from '@/src/types/teamSide';

interface FavoriteGamesListProps {
  favoriteGames: Game[];
  favoriteTournaments: Tournament[];
  toggleFavoriteGame: (gameId: number, isFavorite: boolean) => void;
}

const FavoriteGamesList: React.FC<FavoriteGamesListProps> = ({
  favoriteGames,
  favoriteTournaments,
  toggleFavoriteGame,
}) => {
  const [overrides, setOverrides] = useState<Record<string, { score: any; winner: TeamSide | null }>>({});

  const liveGameIds = useMemo(() => {
    return favoriteGames.filter(g => !g.finished).map(g => String(g.id));
  }, [favoriteGames]);

  const handleRealtimeUpdate = useCallback((gameId: string, dto: { score: any; winner: TeamSide | null }) => {
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

  return (
    <div>
      {favoriteGames.length === 0 ? (
        <p className="text-muted-foreground px-4">Aucun match favori.</p>
      ) : (
        <div className="space-y-2">
          {favoriteGames.map((game) => {
            const override = overrides[String(game.id)];
            let winner: TeamSide | null = null;
            if (override?.winner !== undefined) {
              winner = override.winner;
            } else if (game.finished) {
              winner = game.winnerSide ?? null;
            }

            return (
              <Link
                key={game.id}
                href={`/tournament/${game.tournamentId}/games/${game.id}`}
                className="block"
              >
                <MatchResultCard
                  teamA={game.teamA}
                  teamB={game.teamB}
                  score={override?.score ?? game.score}
                  gameId={String(game.id)}
                  tournamentId={game.tournamentId}
                  editable={false}
                  court={game.court}
                  scheduledTime={game.scheduledTime}
                  winnerSide={winner ?? undefined}
                  stage={''}
                  setsToWin={3}
                  finished={game.finished || winner !== null}
                  matchIndex={undefined}
                  totalMatches={undefined}
                  isFirstRound={false}
                  onInfoSaved={() => {}}
                  isFavorite={true}
                  onToggleFavorite={toggleFavoriteGame}
                />
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default FavoriteGamesList;
