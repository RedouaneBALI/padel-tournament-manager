'use client';

import { useEffect, useState } from 'react';
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import type { Round } from '@/app/types/round';
import type { Game } from '@/app/types/game';
import { fetchRounds } from '@/src/utils/fetchRounds';

interface TournamentGamesTabProps {
  tournamentId: string;
  editable: boolean
}

export default function TournamentGamesTab({ tournamentId, editable }: TournamentGamesTabProps) {
  const [rounds, setRounds] = useState<Round[]>([]);

  const handleInfoSaved = async (result: { tournamentUpdated: boolean; winner: PlayerPair | null }) => {
    if (result.tournamentUpdated) {
      try {
        const rounds = await fetchRounds(tournamentId);
        setRounds(rounds);
      } catch (error) {
        console.error("Erreur lors du rafraîchissement des rounds :", error);
      }
    }
  };


  useEffect(() => {
    async function loadRounds() {
      try {
        const rounds = await fetchRounds(tournamentId);
        setRounds(rounds);
      } catch (err) {
        console.error('Erreur lors du chargement des matchs :', err);
      }
    }

    loadRounds();
  }, [tournamentId]);

  if (rounds.length === 0) {
    return <p className="text-gray-500">Aucun match défini pour le moment.</p>;
  }

return (
  <div className="flex flex-col items-center space-y-4">
    {rounds.flatMap(round =>
      round.games
        .filter(game =>
          (game.teamA !== null || game.teamB !== null) &&
          (game.teamA?.player1?.name !== 'BYE' &&
           game.teamA?.player2?.name !== 'BYE' &&
           game.teamB?.player1?.name !== 'BYE' &&
           game.teamB?.player2?.name !== 'BYE')
        )
        .map(game => (
          <div key={game.id}>
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
              winnerSide={
                game.finished
                  ? game.winnerSide === 'TEAM_A'
                    ? 0
                    : game.winnerSide === 'TEAM_B'
                      ? 1
                      : undefined
                  : undefined
              }
              stage={round.stage}
            />
          </div>
        ))
    )}
  </div>
);
}