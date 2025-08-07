'use client';

import { fetchPairs } from '@/src/api/tournamentApi';
import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import React from 'react';
import PlayerPairList from '@/src/components/tournament/PlayerPairsList';

export default function TournamentPlayersTab({ params }: {   params: Promise<{ id: string }>;}) {
  const { id } = React.use(params);
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);

  useEffect(() => {
    fetchPairs(id)
      .then(setPlayerPairs)
      .catch((error) => {
        console.error("Erreur lors du chargement des joueurs : " + error);
      });
  }, [id]);

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Equipes inscrites</h2>
      <PlayerPairList pairs={playerPairs} />
    </div>
  );
}