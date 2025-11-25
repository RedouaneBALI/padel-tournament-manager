'use client';

import React, { use } from 'react';
import { fetchGame } from '@/src/api/tournamentApi';
import GameDetailView from '@/src/components/game/GameDetailView';

interface PageProps {
  params: Promise<{ id: string; gameId: string }>;
}

export default function AdminGameDetailPage({ params }: PageProps) {
  const { id: tournamentId, gameId } = use(params);

  return (
    <main className="px-4 sm:px-6 py-4 pb-24 min-h-screen">
      <div className="w-full max-w-xl mx-auto">
        <GameDetailView
          gameId={gameId}
          tournamentId={tournamentId}
          fetchGameFn={() => fetchGame(tournamentId, gameId)}
          editable={true}
        />
      </div>
    </main>
  );
}
