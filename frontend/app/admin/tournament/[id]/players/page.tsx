'use client';

import React from 'react';
import { use } from 'react';
import AdminTournamentSetupTab from '@/src/components/admin/AdminTournamentSetupTab';

export default function AdminTournamentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <AdminTournamentSetupTab tournamentId={id} />;
}