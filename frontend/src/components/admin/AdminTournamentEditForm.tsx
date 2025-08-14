'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import TournamentForm from '@/src/components/forms/TournamentForm';
import type { Tournament } from '@/src/types/tournament';
import { fetchTournament, updateTournament } from '@/src/api/tournamentApi';
import { Loader2 } from 'lucide-react';

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

  if (!tournament) {
    return (
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2" />
        //Chargement...
      </div>
    );
  }

  return (
    <TournamentForm
      title="Modifier le tournoi"
      isEditing={true}
      onSubmit={handleUpdate}
      initialData={tournament}
    />
  );
}