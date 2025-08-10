'use client';

import { useEffect, useState, useCallback } from 'react';
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
        setCurrentRoundIndex(0);
      } catch (err) {
        console.error('Erreur lors du chargement des matchs :', err);
      } finally {
        setIsLoading(false);
      }
    }

    loadRounds();
  }, [tournamentId]);

  if (isLoading) {
    return <p className="text-muted">Chargement des matchs...</p>;
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
        onChange={setCurrentRoundIndex}
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
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}