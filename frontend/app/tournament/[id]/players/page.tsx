'use client';

import { fetchPairs } from '@/src/api/tournamentApi';
import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import React from 'react';
import PlayerPairList from '@/src/components/tournament/PlayerPairsList';

export default function TournamentPlayersTab({ params }: {   params: Promise<{ id: string }>;}) {
  const { id } = React.use(params);
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const data = await fetchPairs(id);
        if (!cancelled) setPlayerPairs(data);
      } catch (error) {
        console.error('Erreur lors du chargement des joueurs : ' + error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Equipes inscrites</h2>
      <PlayerPairList pairs={playerPairs} loading={loading} />
    </div>
  );
}