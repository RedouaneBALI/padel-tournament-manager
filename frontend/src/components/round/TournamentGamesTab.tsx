'use client';

import { useEffect, useState } from 'react';
import MatchResultCardLight from '@/src/components/MatchResultCardLight';
import type { Round } from '@/app/types/round';
import type { Game } from '@/app/types/game';

interface TournamentGamesTabProps {
  tournamentId: string;
}

export default function TournamentGamesTab({ tournamentId }: TournamentGamesTabProps) {
  const [games, setGames] = useState<Game[]>([]); // type précisable si tu as un type Game

  useEffect(() => {
    async function fetchRounds() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/rounds`);
        if (!response.ok) throw new Error('Erreur de récupération des rounds');
        const rounds: Round[] = await response.json();

        // Aplatir tous les jeux de tous les rounds
        const allGames = rounds.flatMap(round => round.games);
        setGames(allGames);
      } catch (err) {
        console.error('Erreur lors du chargement des matchs :', err);
      }
    }

    fetchRounds();
  }, [tournamentId]);

  if (games.length === 0) {
    return <p className="text-gray-500">Aucun match défini pour le moment.</p>;
  }

return (
  <div className="flex flex-col items-center space-y-4">
    {games.map((game) => (
      <MatchResultCardLight
        key={game.id}
        teamA={game.teamA}
        teamB={game.teamB}
        score={game.score}
        gameId={game.id}
        tournamentId={tournamentId}
        editable={true}
      />
    ))}
  </div>
);
}