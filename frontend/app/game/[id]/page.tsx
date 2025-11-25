// app/game/[id]/page.tsx
'use client';

import React, { use } from 'react';
import { fetchStandaloneGame } from '@/src/api/tournamentApi';
import GamePageShell from '@/src/components/game/GamePageShell';

interface PageProps {
  params: Promise<{ id: string; id: string }>;
}

export default function GameDetailPage({ params }: PageProps) {
  const { id } = use(params);

  return (
    <GamePageShell
      gameId={id}
      fetchGameFn={() => fetchStandaloneGame(id)}
      editable={false}
      includeViewers={true}
      includeBottomNav={true}
    />
  );
}
