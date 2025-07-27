'use client';

import React from 'react';
import { use } from 'react';
import AdminTournamentEditForm from '@/src/components/admin/AdminTournamentEditForm';

export default function AdminEditTournamentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <AdminTournamentEditForm tournamentId={id} />;
}