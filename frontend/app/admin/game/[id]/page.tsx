// app/admin/game/[id]/page.tsx
'use client';

import React, { use } from 'react';
import { fetchStandaloneGame, updateStandaloneGame } from '@/src/api/tournamentApi';
import GamePageShell from '@/src/components/game/GamePageShell';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import { ExportProvider } from '@/src/contexts/ExportContext';

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function StandaloneGameDetailPage({ params }: PageProps) {
  const { id: gameId } = use(params);

  return (
    <ExportProvider>
      <main className="px-4 sm:px-6 py-4">
        <AdminTournamentHeader tournament={null} />

        <div className="w-full max-w-xl mx-auto">
          <GamePageShell
            gameId={gameId}
            fetchGameFn={() => fetchStandaloneGame(gameId)}
            updateGameFn={(gId, score, court, scheduledTime) =>
              updateStandaloneGame(gId, score, court, scheduledTime)
            }
            editable={true}
            includeViewers={true}
            includeBottomNav={true}
          />
        </div>

        <ToastContainer />
      </main>
    </ExportProvider>
  );
}
