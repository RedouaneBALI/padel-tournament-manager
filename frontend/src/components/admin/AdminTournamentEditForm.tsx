'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { toast } from 'react-toastify';
import TournamentForm from '@/src/components/forms/TournamentForm';
import type { Tournament } from '@/src/types/tournament';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentEditForm({ tournamentId }: Props) {
  const router = useRouter();
  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    async function fetchTournament() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${tournamentId}`);
        if (!res.ok) throw new Error('Erreur lors de la récupération du tournoi');
        const data: Tournament = await res.json();
        setTournament(data);
      } catch (err) {
        toast.error('Impossible de charger les infos du tournoi : ' + err);
      }
    }

    fetchTournament();
  }, [tournamentId]);

  const handleUpdate = async (updatedTournament: Tournament) => {
    try {
      const res = await fetch(`http://localhost:8080/tournaments/${tournamentId}`, {
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