// app/game/[id]/page.tsx
'use client';

import React, { use } from 'react';
import { fetchStandaloneGame } from '@/src/api/tournamentApi';
import GameDetailView from '@/src/components/game/GameDetailView';

interface PageProps {
  params: Promise<{ id: string; id: string }>;
}

export default function GameDetailPage({ params }: PageProps) {
  const { id: id } = use(params);

  return (
    <GameDetailView
      gameId={id}
      fetchGameFn={() => fetchStandaloneGame(id)}
      editable={false}
    />
  );
}
