'use client';

import { useEffect, useState } from 'react';
import React from 'react';
import { toast } from 'react-toastify';
import { Round } from '@/src/types/round';


export default function TournamentResultsTab({ params }: { params: Promise<{ id: string }> }) {
  const { id } = React.use(params);
  const [rounds, setRounds] = useState<Round[]>([]);
  console.log(rounds);

  useEffect(() => {
    async function fetchRounds() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${id}/rounds`);
        if (!response.ok) throw new Error();
        const data: Round[] = await response.json();
        setRounds(data);
      } catch (err) {
        toast.error("Impossible de récupérer les rounds.");
      }
    }
    fetchRounds();
  }, [id]);

return (
  <div className="space-y-6">
    <h2 className="text-xl font-semibold text-foreground">Résultats</h2>

    {rounds.length === 0 ? (
      <p className="text-muted-foreground">Aucun tirage généré pour le moment.</p>
    ) : (
      rounds.map((round) => (
        <div key={round.id} className="p-4 border rounded-md shadow-sm space-y-4 bg-muted/50">
          <h3 className="text-lg font-bold text-primary">{round.name}</h3>

          {round.games?.length > 0 ? (
            <div className="space-y-3">
              {round.games.map((game: any) => (
                <div key={game.id} className="p-3 border rounded-md bg-background shadow-sm">
                  <div className="flex justify-between items-center">
                    <div>
                      <p className="font-medium text-foreground">
                        {game.teamA?.player1?.name} & {game.teamA?.player2?.name}
                        {game.teamA?.seed != null && (
                          <span className="ml-2 text-sm text-muted-foreground">(TS {game.teamA.seed})</span>
                        )}
                      </p>
                      <p className="text-muted-foreground text-sm">vs</p>
                      <p className="font-medium text-foreground">
                        {game.teamB?.player1?.name} & {game.teamB?.player2?.name}
                        {game.teamB?.seed != null && (
                          <span className="ml-2 text-sm text-muted-foreground">(TS {game.teamB.seed})</span>
                        )}
                      </p>
                    </div>
                    <div className="text-sm text-muted-foreground">
                      {/* Tu peux ajouter ici le score ou le terrain plus tard */}
                      {game.court ? `Terrain : ${game.court}` : ''}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-muted-foreground">Aucun match prévu dans ce round.</p>
          )}
        </div>
      ))
    )}
  </div>
);
}