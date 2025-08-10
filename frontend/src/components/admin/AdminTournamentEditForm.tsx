'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import TournamentForm from '@/src/components/forms/TournamentForm';
import type { Tournament } from '@/src/types/tournament';
import { fetchTournament, updateTournament } from '@/src/api/tournamentApi';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentEditForm({ tournamentId }: Props) {
  const router = useRouter();
  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    async function loadTournament() {
      try {
        const data: Tournament = await fetchTournament(tournamentId);
        setTournament(data);
      } catch (err) {
      }
    }

    loadTournament();
  }, [tournamentId]);

  const handleUpdate = async (updatedTournament: Tournament) => {
  await updateTournament(tournamentId, updatedTournament);
  router.refresh();

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