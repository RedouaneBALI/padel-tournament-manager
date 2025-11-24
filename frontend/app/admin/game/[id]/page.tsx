'use client';

import React, { use } from 'react';
import { fetchStandaloneGame, updateStandaloneGame } from '@/src/api/tournamentApi';
import GameDetailView from '@/src/components/game/GameDetailView';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import BottomNav from '@/src/components/ui/BottomNav';
import { useRouter, usePathname } from 'next/navigation';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function StandaloneGameDetailPage({ params }: PageProps) {
  const { id: gameId } = use(params);
  const items = getDefaultBottomItems();
  const pathname = usePathname() ?? '';

  return (
    <>
      <GameDetailView
        gameId={gameId}
        fetchGameFn={() => fetchStandaloneGame(gameId)}
        updateGameFn={(gId, score, court, scheduledTime) =>
          updateStandaloneGame(gId, score, court, scheduledTime)
        }
        editable={true}
        showTvButton={true}
        tvButtonUrl={`/tv/game/${gameId}`}
      />
      <ToastContainer />
      <BottomNav items={items} pathname={pathname} />
    </>
  );
}

