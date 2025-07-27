'use client';

import React,{use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import TournamentForm from '@/src/components/forms/TournamentForm';
import type { Tournament } from '@/src/types/tournament';
import { toast } from 'react-toastify';

export default function AdminEditTournamentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [tournament, setTournament] = useState<Tournament | null>(null);
  console.log(tournament); // works

  useEffect(() => {
    async function fetchTournament() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}`);
        if (!res.ok) throw new Error('Erreur lors de la récupération du tournoi');
        const data: Tournament = await res.json();
        setTournament(data);
      } catch (err) {
        toast.error('Impossible de charger les infos du tournoi : ' + err);
      }
    }

    fetchTournament();
  }, [id]);

  const handleUpdate = async (updatedTournament: Tournament) => {
    try {
      const res = await fetch(`http://localhost:8080/tournaments/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updatedTournament),
      });

      if (!res.ok) throw new Error();
      toast.success('Tournoi mis à jour !');
      router.refresh();
    } catch {
      toast.error('Erreur lors de la mise à jour.');
    }
  };

  if (!tournament) return <div>Chargement...</div>;

  return (
    <TournamentForm
      title="Modifier le tournoi"
      isEditing={true}
      onSubmit={handleUpdate}
      initialData={tournament}
    />
  );
}