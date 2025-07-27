'use client';

import React, {use, useEffect, useState } from 'react';
import PlayerPairsTextarea from '@/src/components/tournament/PlayerPairsTextarea';
import { PlayerPair } from '@/src/types/playerPair';
import { toast } from 'react-toastify';

export default function AdminPlayersPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [pairs, setPairs] = useState<PlayerPair[]>([]);

  useEffect(() => {
    async function fetchPairs() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}/pairs`);
        if (!res.ok) throw new Error();
        const data: PlayerPair[] = await res.json();
        setPairs(data);
      } catch {
        toast.error('Erreur lors du chargement des joueurs.');
      }
    }
    fetchPairs();
  }, [id]);

  return (
    <div>
      <h2 className="mb-3 text-base font-semibold text-foreground">
        Lister les joueurs (par ordre de classement)
      </h2>
      <PlayerPairsTextarea
        onPairsChange={setPairs}
        tournamentId={Number(id)}
      />
    </div>
  );
}