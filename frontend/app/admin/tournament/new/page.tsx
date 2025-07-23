'use client';

import { useRouter } from 'next/navigation';
import TournamentForm from '@/components/forms/TournamentForm';
import { toast } from 'react-toastify';

export default function NewTournamentPage() {
  const router = useRouter();

  const handleCreate = async (data) => {
    const res = await fetch('http://localhost:8080/tournaments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      toast.error("Erreur lors de la création du tournoi.");
      return;
    }

    const tournament = await res.json();
    toast.success('Tournoi créé !');
    router.push(`/admin/tournament/${tournament.id}`);
  };

  return (
    <TournamentForm
      title="Créer un nouveau tournoi"
      isEditing={false}
      onSubmit={handleCreate}
    />
  );
}