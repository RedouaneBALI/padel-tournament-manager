'use client';

import React from 'react';
import { use } from 'react';
import AdminTournamentSetupTab from '@/src/components/admin/AdminTournamentSetupTab';
import TournamentGamesTab from '@/src/components/round/TournamentGamesTab';

export default function TournamentGamesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <TournamentGamesTab tournamentId={id} editable={false}/>;
}