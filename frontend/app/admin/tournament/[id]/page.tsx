'use client';

import { useEffect, useState } from 'react';
import React from 'react';
import { toast, ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/components/tournament/PlayerPairsTextarea';
import Link from 'next/link';
import { FileText, Settings } from 'lucide-react';
import { PlayerPair } from '@/types/playerPair';


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

export default function TournamentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = React.use(params);
  const baseUrl = typeof window !== 'undefined' ? window.location.origin : '';
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
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
          const data: PlayerPair[] = await response.json();
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

  async function handleDraw() {
    try {
      const response = await fetch(`http://localhost:8080/tournaments/${id}/draw`, {
        method: 'POST',
      });

      if (response.ok) {
        toast.success('Tirage au sort généré avec succès !');
      } else {
        toast.error('Erreur lors de la génération du tirage.');
      }
    } catch (error) {
      toast.error('Erreur réseau.');
      console.error(error);
    }
  }

  return (
    <div className="container mx-auto max-w-3xl">
      <div className="bg-card shadow-sm p-6 space-y-6">
        {/* Header with icon and title */}
        <div className="flex items-center gap-2 border-b border-border pb-3">
          <FileText className="h-5 w-5 text-muted-foreground" />
          <h1 className="text-xl font-semibold text-foreground">
            {tournament?.name || `Tournoi #${id}`}
          </h1>
          <Link
            href={`/admin/tournament/${id}/edit`}
            className="ml-auto inline-flex items-center gap-1 rounded bg-primary px-3 py-1 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition"
          >
            <Settings className="h-6 w-6" />
          </Link>
        </div>

        {/* Share link */}
        <section className="space-y-1">
          <p className="text-sm text-muted-foreground">Partagez ce lien avec les joueurs :</p>
          <div className="flex gap-2">
            <input
              readOnly
              value={`${baseUrl}/tournament/${id}`}
              className="flex-grow rounded border border-input bg-background px-3 py-2 text-sm text-foreground shadow-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent"
            />
            <button
              onClick={copyLink}
              className="rounded bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition"
            >
              Copier
            </button>
          </div>
        </section>

        {/* Player pairs textarea */}
        <section>
          <h2 className="mb-3 text-base font-semibold text-foreground">
            Lister les joueurs ci-dessous (par ordre de classement)
          </h2>
          <PlayerPairsTextarea tournamentId={Number(id)} defaultPairs={pairs} />
        </section>
        <button
          onClick={handleDraw}
          className="w-full sm:w-auto mt-4 px-4 py-2 bg-emerald-600 text-white rounded-md hover:bg-emerald-700 transition-colors font-medium text-sm"
        >
          Générer le tirage
        </button>
      </div>

      <ToastContainer />
    </div>
  );
}