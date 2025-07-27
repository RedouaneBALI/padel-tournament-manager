'use client';

import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { Round } from '@/src/types/round';
import { Game } from '@/src/types/game';
import MatchResultCardLight from '@/src/components/MatchResultCardLight';

interface TournamentResultsTabProps {
  tournamentId: string;
}

export default function TournamentResultsTab({ tournamentId }: TournamentResultsTabProps) {
  const [rounds, setRounds] = useState<Round[]>([]);

  useEffect(() => {
    async function fetchRounds() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/rounds`);
        if (!response.ok) throw new Error();
        const data: Round[] = await response.json();
        setRounds(data);
      } catch (err) {
        toast.error("Erreur lors du chargement des rounds : " + err);
      }
    }

    fetchRounds();
  }, [tournamentId]);

  if (rounds.length === 0) {
    return <p className="text-muted-foreground">Aucun tirage généré pour le moment.</p>;
  }

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-foreground">Arbre du tournoi</h2>

      {rounds.map((round) => (
        <div key={round.id} className="p-4 border rounded-md shadow-sm space-y-4 bg-muted/50">
          <h3 className="text-lg font-bold text-primary">{round.stage}</h3>

          {round.games?.length > 0 ? (
            <div className="space-y-3">
              {round.games.map((game: Game) => (
                <MatchResultCardLight key={game.id} teamA={game.teamA} teamB={game.teamB} />
              ))}
            </div>
          ) : (
            <p className="text-muted-foreground">Aucun match dans ce round.</p>
          )}
        </div>
      ))}
    </div>
  );
}