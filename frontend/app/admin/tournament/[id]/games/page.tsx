// app/admin/tournament/[id]/games/page.tsx
'use client';

import TournamentGamesTab from '@/src/components/tournament/games/TournamentGamesTab';
import React from 'react';
import { use } from 'react';

export default function AdminTournamentGamesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <TournamentGamesTab tournamentId={id} editable={true} />;
}