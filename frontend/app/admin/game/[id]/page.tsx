// app/admin/game/[id]/page.tsx
'use client';

import React, { use, useEffect, useState } from 'react';
import { fetchStandaloneGame, updateStandaloneGame } from '@/src/api/tournamentApi';
import GamePageShell from '@/src/components/game/GamePageShell';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import { ExportProvider } from '@/src/contexts/ExportContext';
import { useRouter } from 'next/navigation';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function StandaloneGameDetailPage({ params }: PageProps) {
  const { id: gameId } = use(params);
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    async function checkAuthorization() {
      try {
        const game = await fetchStandaloneGame(gameId);
        if (!mounted) return;

        // Only isEditable === true grants access
        if (game.isEditable !== true) {
          router.replace(`/game/${gameId}`);
          return;
        }

        setAuthorized(true);
      } catch (e: any) {
        if (!mounted) return;

        if (e?.message === 'FORBIDDEN' || e?.message === 'UNAUTHORIZED') {
          router.replace(`/game/${gameId}`);
          return;
        }

        // Other errors
        router.replace('/500');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    checkAuthorization();
    return () => { mounted = false; };
  }, [gameId, router]);

  if (loading || !authorized) {
    return (
      <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
        <CenteredLoader />
        <ToastContainer />
      </div>
    );
  }

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
