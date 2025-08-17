// app/admin/tournament/[id]/edit/page.tsx
'use client';

import { use } from 'react';
import AdminTournamentForm from '@/src/components/admin/AdminTournamentForm';

export default function AdminEditTournamentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <AdminTournamentForm tournamentId={id} />;
}