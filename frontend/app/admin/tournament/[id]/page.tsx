'use client';

import { useEffect, useState } from 'react';
import React from 'react';
import { toast, ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/components/tournament/PlayerPairsTextarea';
import Link from 'next/link';

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
          toast.error('Impossible de récupérer les infos du tournoi.');
        }
      } catch {
        toast.error('Erreur réseau lors de la récupération du tournoi.');
      }
    }

    fetchTournament();
  }, [id]);

  useEffect(() => {
    async function fetchPairs() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${id}/pairs`);
        if (response.ok) {
          const data: PlayerPairInput[] = await response.json();
          setPairs(data);
        } else {
          toast.error('Impossible de récupérer les joueurs existants.');
        }
      } catch {
        toast.error('Erreur réseau lors de la récupération des joueurs.');
      }
    }

    fetchPairs();
  }, [id]);

  async function copyLink() {
    if (baseUrl) {
      await navigator.clipboard.writeText(`${baseUrl}/tournament/${id}`);
      toast.success('Lien copié dans le presse-papiers !');
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">
        {tournament?.name ? tournament.name : `Tournoi #${id}`}
      </h1>

      <section className="mb-8">
        <p>Partagez ce lien avec les joueurs :</p>
        <div className="flex items-center gap-2 mt-2">
          <input
            readOnly
            value={`${baseUrl}/tournament/${id}`}
            className="flex-1 border px-3 py-2 rounded"
          />
          <button
            onClick={copyLink}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Copier
          </button>
        </div>
      </section>

      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-4">
          Lister les joueurs ci-dessous (par ordre de classement)
        </h2>

        <PlayerPairsTextarea tournamentId={Number(id)} defaultPairs={pairs} />
        <ToastContainer />
      </section>
      <div className="flex justify-between items-center mb-4">
        <Link
          href={`/admin/tournament/${id}/edit`}
          className="bg-yellow-500 hover:bg-yellow-600 text-white font-medium px-4 py-2 rounded shadow"
        >
          Modifier le tournoi
        </Link>
      </div>
    </div>
  );
}