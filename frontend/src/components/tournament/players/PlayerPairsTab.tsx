'use client';

import { useEffect, useState } from 'react';
import { fetchPairs } from '@/src/api/tournamentApi';
import type { PlayerPair } from '@/src/types/playerPair';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface Props {
  tournamentId: string;
}

export default function PlayerPairsTab({ tournamentId }: Props) {
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetchPairs(tournamentId)
      .then(setPlayerPairs)
      .catch((err) => {
        console.error("Erreur lors du chargement des paires :", err);
        setError('Impossible de charger les équipes.');
      })
      .finally(() => setLoading(false));
  }, [tournamentId]);

  if (error) {
    return <p className="text-red-500 italic">{error}</p>;
  }

  if (loading) {
    return <CenteredLoader />;
  }

  if (!loading && playerPairs.length === 0) {
    return <p className="text-muted italic">Aucune paire inscrite pour le moment.</p>;
  }

  return (
    <ul className="space-y-2">
      {playerPairs.map((pair, index) => (
        <li key={index} className="border rounded px-4 py-2 bg-background shadow-sm">
          <span className="font-semibold text-primary">
            {pair.seed && pair.seed > 0 ? `#${pair.seed} ` : ''}
          </span>
          {pair.player1Name} – {pair.player2Name}
        </li>
      ))}
    </ul>
  );
}