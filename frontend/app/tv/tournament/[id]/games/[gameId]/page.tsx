'use client';

import React, { use } from 'react';
import { fetchGame } from '@/src/api/tournamentApi';
import TVModeView from '@/src/components/game/TVModeView';

interface PageProps {
  params: Promise<{ id: string; gameId: string }>;
}

export default function TVModePage({ params }: PageProps) {
  const { id: tournamentId, gameId } = use(params);

  return (
    <TVModeView
      gameId={gameId}
      fetchGameFn={() => fetchGame(tournamentId, gameId)}
    />
  );
}

