'use client';

import type { ParsedTournamentForm } from '@/src/validation/tournament';

import { useEffect, useState, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import TournamentForm from '@/src/components/forms/TournamentForm';
import type { Tournament } from '@/src/types/tournament';
import { createTournament, fetchTournament, updateTournament } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import BottomNav from '@/src/components/ui/BottomNav';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import { usePathname } from 'next/navigation';

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

  const handleCreate = async (data: ParsedTournamentForm) => {
    const created = await createTournament(data as unknown as Tournament);
    console.log(data);
    router.push(`/admin/tournament/${created.id}/players`);
  };

  const handleUpdate = async (data: ParsedTournamentForm) => {
    if (!tournamentId) return;
    await updateTournament(tournamentId, data as unknown as Tournament);
    router.refresh();
  };

  const isEditing = Boolean(tournamentId);
  const title = isEditing ? 'Modifier le tournoi' : 'Créer un tournoi';

  const initialData = useMemo(() => {
    if (isEditing) {
      return tournament ?? undefined;
    } else {
      return undefined;
    }
  }, [isEditing, tournament]);

  if (loading) {
    return <CenteredLoader />;
  }

  return (
    <>
      <TournamentForm
        title={title}
        isEditing={isEditing}
        onSubmit={isEditing ? handleUpdate : handleCreate}
        initialData={initialData}
      />
      <BottomNav items={items} pathname={pathname} />
    </>
  );
}
