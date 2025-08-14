'use client';

import React from 'react';
import { use } from 'react';
import TournamentGamesTab from '@/src/components/tournament/games/TournamentGamesTab';

export default function TournamentGamesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return <TournamentGamesTab tournamentId={id} editable={false}/>;
}