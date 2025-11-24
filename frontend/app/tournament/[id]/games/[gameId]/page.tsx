'use client';

import React, { use } from 'react';
import { fetchGame } from '@/src/api/tournamentApi';
import GameDetailView from '@/src/components/game/GameDetailView';

interface PageProps {
  params: Promise<{ id: string; gameId: string }>;
}

export default function GameDetailPage({ params }: PageProps) {
  const { id: tournamentId, gameId } = use(params);

  return (
    <GameDetailView
      gameId={gameId}
      tournamentId={tournamentId}
      fetchGameFn={() => fetchGame(tournamentId, gameId)}
      editable={false}
    />
  );
}
