'use client';

import React from 'react';
import { use } from 'react';
import TournamentGamesTab from '@/src/components/round/TournamentGamesTab';

export default function AdminGamesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <TournamentGamesTab tournamentId={id} editable={true}/>;
}