'use client';

import React from 'react';
import GameDetailView from '@/src/components/game/GameDetailView';
import ViewersCounter from '@/src/components/ui/ViewersCounter';
import BottomNav from '@/src/components/ui/BottomNav';
import { usePathname } from 'next/navigation';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';

interface Props {
  gameId: string;
  tournamentId?: string;
  fetchGameFn: () => Promise<any>;
  updateGameFn?: (gameId: string, score: any, court: string, scheduledTime: string) => Promise<any>;
  editable?: boolean;
  title?: string;
  includeViewers?: boolean;
  includeBottomNav?: boolean;
}

export default function GamePageShell({
  gameId,
  tournamentId,
  fetchGameFn,
  updateGameFn,
  editable = false,
  title,
  includeViewers = true,
  includeBottomNav = true,
}: Props) {
  const items = getDefaultBottomItems();
  const pathname = usePathname() ?? '';

  return (
    <>
      <div className="w-full max-w-xl mx-auto">
        <GameDetailView
          gameId={gameId}
          tournamentId={tournamentId}
          fetchGameFn={fetchGameFn}
          updateGameFn={updateGameFn}
          editable={editable}
          title={title}
        />
      </div>

      {includeViewers && <ViewersCounter gameId={gameId} />}

      {includeBottomNav && <BottomNav items={items} pathname={pathname} />}
    </>
  );
}
