'use client';

import React from 'react';
import { use } from 'react';
import TournamentResultsTab from '@/src/components/round/TournamentResultsTab';

export default function AdminTournamentResultsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <TournamentResultsTab tournamentId={id} editable={true} />;
}