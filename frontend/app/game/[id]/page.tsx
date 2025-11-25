// app/game/[id]/page.tsx
'use client';

import React from 'react';
import { useParams } from 'next/navigation';
import { fetchStandaloneGame } from '@/src/api/tournamentApi';
import GamePageShell from '@/src/components/game/GamePageShell';

export default function GameDetailPage() {
  const params = useParams();
  const id = params?.id as string | undefined;

  if (!id) {
    return <div>Identifiant de match manquant</div>;
  }

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
