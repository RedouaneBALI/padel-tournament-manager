'use client';

import { useRouter } from 'next/navigation';
import TournamentForm from './TournamentForm';
import { toast } from 'react-toastify';
import type { Tournament } from '@/src/types/tournament';
import { createTournament } from '@/src/api/tournamentApi';

export default function CreateTournamentForm() {
  const router = useRouter();

  const handleCreate = async (data: Tournament) => {
    const tournament = await createTournament(data);
    router.push(`/admin/tournament/${tournament.id}/players`);
  };

  return (
    <TournamentForm
      title="Créer un tournoi"
      isEditing={false}
      onSubmit={handleCreate}
    />
  );
}
