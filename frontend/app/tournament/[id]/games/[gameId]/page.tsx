'use client';

import React, { use } from 'react';
import { fetchGame } from '@/src/api/tournamentApi';
import GamePageShell from '@/src/components/game/GamePageShell';

interface PageProps {
  params: Promise<{ id: string; gameId: string }>;
}

export default function GameDetailPage({ params }: PageProps) {
  const { id: tournamentId, gameId } = use(params);

  return (
    <GamePageShell
      gameId={gameId}
      tournamentId={tournamentId}
      fetchGameFn={() => fetchGame(tournamentId, gameId)}
      editable={false}
      includeViewers={false}
      includeBottomNav={false}
    />
  );
}
