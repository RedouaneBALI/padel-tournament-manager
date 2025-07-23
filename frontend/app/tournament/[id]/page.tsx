'use client';

import { useEffect, useState } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import React from 'react';
import TournamentTabs from '@/components/tournament/TournamentTabs';

interface PlayerPairInput {
  player1: string;
  player2: string;
}

interface Tournament {
  id: number;
  name: string;
  description?: string;
  city?: string;
  club?: string;
  gender?: string;
  level?: string;
  startDate?: string;
  endDate?: string;
}

export default function TournamentPage({ params }: { params: { id: string } }) {
  const { id } = React.use(params);
  const baseUrl = typeof window !== 'undefined' ? window.location.origin : '';
  const [pairs, setPairs] = useState<PlayerPairInput[]>([]);
  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    async function fetchTournament() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${id}`);
        if (response.ok) {
          const data: Tournament = await response.json();
          setTournament(data);
        } else {
          toast.error("Impossible de récupérer les infos du tournoi.");
        }
      } catch {
        toast.error("Erreur réseau lors de la récupération du tournoi.");
      }
    }

    async function fetchPairs() {
      try {
        console.log("fetchPairs");
        const response = await fetch(`http://localhost:8080/tournaments/${id}/pairs`);
        if (response.ok) {
          const data: PlayerPairInput[] = await response.json();
          setPairs(data);
        } else {
          toast.error('Impossible de récupérer les joueurs.');
        }
      } catch {
        toast.error('Erreur réseau lors de la récupération des joueurs.');
      }
    }

    fetchTournament();
    fetchPairs();
  }, [id]);

  async function copyLink() {
    if (baseUrl) {
      await navigator.clipboard.writeText(`${baseUrl}/tournament/${id}`);
      toast.success('Lien copié dans le presse-papiers !');
    }
  }
    return (
      <div className="max-w-4xl mx-auto p-6">
        <h1 className="text-2xl font-bold mb-4">Tournoi #{id}</h1>
        {tournament && <TournamentTabs tournament={tournament} />}
        <ToastContainer />
      </div>
  );
}