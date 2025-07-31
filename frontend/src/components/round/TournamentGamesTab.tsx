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
  const [games, setGames] = useState<Game[]>([]); // type précisable si tu as un type Game

  const handleScoreSaved = (result: { tournamentUpdated: boolean; winner: PlayerPair | null }) => {
        console.log("handleScoreSaved");
        console.log(result);
        if (result.tournamentUpdated) {
          fetchRounds(tournamentId);
        }
      };


    useEffect(() => {
      async function loadRounds() {
        try {
          console.log("tournamentId : " + tournamentId);
          const rounds = await fetchRounds(tournamentId);
          const allGames = rounds.flatMap(round => round.games);
          setGames(allGames);
        } catch (err) {
          console.error('Erreur lors du chargement des matchs :', err);
        }
      }

      loadRounds();
    }, [tournamentId]);

  if (games.length === 0) {
    return <p className="text-gray-500">Aucun match défini pour le moment.</p>;
  }

return (
  <div className="flex flex-col items-center space-y-4">
    {games
      .filter(game =>
        (game.teamA !== null || game.teamB !== null) &&
        (game.teamA?.player1?.name !== 'BYE' &&
         game.teamA?.player2?.name !== 'BYE' &&
         game.teamB?.player1?.name !== 'BYE' &&
         game.teamB?.player2?.name !== 'BYE')
      )
      .map((game) => (
        <MatchResultCard
          key={game.id}
          teamA={game.teamA}
          teamB={game.teamB}
          score={game.score}
          gameId={game.id}
          tournamentId={tournamentId}
          editable={editable}
          onScoreSaved={handleScoreSaved}
        />
      ))}
  </div>
);
}