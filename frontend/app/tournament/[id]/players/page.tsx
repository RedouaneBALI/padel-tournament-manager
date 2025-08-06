'use client';

import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import React from 'react';

export default function TournamentPlayersTab({ params }: {   params: Promise<{ id: string }>;}) {
  const { id } = React.use(params);
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);

  useEffect(() => {
    async function fetchPairs() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${id}/pairs`);
        if (!response.ok) throw new Error();
        const data = await response.json();
        setPlayerPairs(data);
      } catch (error) {
        console.error("Erreur lors du chargement des joueurs : " + error);
      }
    }

    fetchPairs();
  }, [id]);

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Equipes inscrites</h2>

      {playerPairs.length === 0 ? (
        <p className="text-gray-500 italic">Aucune paire inscrite pour le moment.</p>
      ) : (
        <ul className="space-y-2">
          {playerPairs.map((pair, index) => (
            <li key={index} className="border rounded px-4 py-2 bg-gray-50 shadow-sm">
              <span className="font-semibold text-primary">
                {pair.seed && pair.seed > 0 ? `#${pair.seed} ` : ''}
              </span>
              {pair.player1.name} – {pair.player2.name}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}