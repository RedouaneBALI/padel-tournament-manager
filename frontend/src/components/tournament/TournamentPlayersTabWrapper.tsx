'use client';

import { useEffect, useState } from 'react';
import { Tournament } from '@/app/types/Tournament';
import { SimplePlayerPair } from '@/app/types/PlayerPair';

interface Props {
  tournament: Tournament;
}

export default function TournamentPlayersTabWrapper({ tournament }: Props) {
  const [playerPairs, setPlayerPairs] = useState<SimplePlayerPair[]>([]);

  useEffect(() => {
    async function fetchPairs() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${tournament.id}/pairs`);
        if (!response.ok) throw new Error();
        const data = await response.json();
        setPlayerPairs(data);
      } catch (error) {
        console.error("Erreur lors du chargement des joueurs.");
      }
    }

    fetchPairs();
  }, [tournament.id]);

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Joueurs inscrits</h2>

      {playerPairs.length === 0 ? (
        <p className="text-gray-500 italic">Aucune paire inscrite pour le moment.</p>
      ) : (
        <ul className="space-y-2">
          {playerPairs.map((pair, index) => (
            <li key={index} className="border rounded px-4 py-2 bg-gray-50 shadow-sm">
              {pair.player1} &nbsp;–&nbsp; {pair.player2}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}