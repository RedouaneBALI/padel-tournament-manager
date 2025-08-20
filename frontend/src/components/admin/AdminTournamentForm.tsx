// src/components/admin/AdminTournamentForm.tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import TournamentForm from '@/src/components/forms/TournamentForm';
import type { Tournament } from '@/src/types/tournament';
import { createTournament, fetchTournament, updateTournament } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import BottomNav from '@/src/components/ui/BottomNav';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import { usePathname } from 'next/navigation';
import type { TournamentPayload } from '@/src/validation/tournament';

interface Props {
  /** If provided, the form is in edit mode; otherwise, creation mode */
  tournamentId?: string;
}

export default function AdminTournamentForm({ tournamentId }: Props) {
  const router = useRouter();
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [loading, setLoading] = useState<boolean>(!!tournamentId);

  const pathname = usePathname() ?? '';
  const items = getDefaultBottomItems();

  // If editing, load tournament once
  useEffect(() => {
    if (!tournamentId) return;
    let mounted = true;
    (async () => {
      try {
        const data = await fetchTournament(tournamentId);
        if (mounted) setTournament(data);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [tournamentId]);

  const handleCreate = async (data: TournamentPayload) => {
    const created = await createTournament(data as unknown as Tournament);
    router.push(`/admin/tournament/${created.id}/players`);
  };

  const handleUpdate = async (data: TournamentPayload) => {
    if (!tournamentId) return;
    await updateTournament(tournamentId, data as unknown as Tournament);
    router.refresh();
  };

  if (loading) {
    return <CenteredLoader />;
  }

  const isEditing = Boolean(tournamentId);

  return (
    <>
      <TournamentForm
        title={isEditing ? 'Modifier le tournoi' : 'Créer un tournoi'}
        isEditing={isEditing}
        onSubmit={isEditing ? handleUpdate : handleCreate}
        initialData={isEditing ? tournament ?? undefined : undefined}
      />
      <BottomNav items={items} pathname={pathname} />
    </>
  );
}
