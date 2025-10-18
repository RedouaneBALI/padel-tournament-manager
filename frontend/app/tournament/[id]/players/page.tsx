'use client';

import { fetchPairs } from '@/src/api/tournamentApi';
import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import React from 'react';
import PlayerPairsList from '@/src/components/tournament/players/PlayerPairsList';

export default function TournamentPlayersTab({ params }: {   params: Promise<{ id: string }>;}) {
  const { id } = React.use(params);
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const data = await fetchPairs(id, false, false);
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
    <PlayerPairsList pairs={playerPairs} loading={loading} tournamentId={id} />
  );
}