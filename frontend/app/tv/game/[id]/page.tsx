'use client';

import React, { use } from 'react';
import { fetchStandaloneGame } from '@/src/api/tournamentApi';
import TVModeView from '@/src/components/game/TVModeView';

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function TVModeStandalonePage({ params }: PageProps) {
  const { id: gameId } = use(params);

  return (
    <TVModeView
      gameId={gameId}
      fetchGameFn={() => fetchStandaloneGame(gameId)}
      title="PadelRounds.com"
    />
  );
}

