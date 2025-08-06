'use client';

import React, { useEffect, useState } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/src/components/tournament/PlayerPairsTextarea';
import { FileText } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { fetchTournament, fetchPairs } from '@/src/api/tournamentApi';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentSetupTab({ tournamentId }: Props) {
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    async function loadTournament() {
      try {
        const data = await fetchTournament(tournamentId);
        setTournament(data);
      } catch {
        toast.error('Erreur réseau lors de la récupération du tournoi.');
      }
    }
    loadTournament();
  }, [tournamentId]);

  useEffect(() => {
    async function loadPairs() {
      try {
        const data = await fetchPairs(tournamentId);
        setPairs(data);
      } catch {
        toast.error('Erreur réseau lors de la récupération des joueurs.');
      }
    }
    loadPairs();
  }, [tournamentId]);


  return (
    <div className="container mx-auto max-w-3xl">
      <div className="bg-card shadow-sm p-6 space-y-6">
        <div className="flex items-center gap-2 border-b border-border pb-3">
          <FileText className="h-5 w-5 text-muted-foreground" />
          <h1 className="text-xl font-semibold text-foreground">
            {pairs.length} joueurs
          </h1>
        </div>

        <section>
          <h2 className="mb-3 text-base font-semibold text-foreground">
            Lister les joueurs ci-dessous (par ordre de classement)
          </h2>
          <PlayerPairsTextarea
            onPairsChange={setPairs}
            tournamentId={Number(tournamentId)}
          />
        </section>

      </div>

      <ToastContainer />
    </div>
  );
}