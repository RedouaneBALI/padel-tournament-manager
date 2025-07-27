'use client';

import React from 'react';
import { PlayerPair } from '@/types/playerPair';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
}

export default function MatchResultCardLight({ teamA, teamB }: Props) {
  const renderPair = (pair: PlayerPair | null) => {
    return (
      <div className="flex items-center justify-between px-4 h-[60px]">
        <div className="flex flex-col flex-1">
          <span className="text-sm text-foreground truncate">
            {pair?.player1?.name || ''}
          </span>
          <span className="text-sm text-muted-foreground truncate">
            {pair?.player2?.name || ''}
          </span>
        </div>

        <div className="flex items-center space-x-2">
          {pair?.seed && (
            <span className="text-xs bg-primary/10 text-primary px-2 py-1 rounded-full font-medium">
              #{pair.seed}
            </span>
          )}
          <div className="w-12 text-center">
            {/* Score ou placeholder */}
            <div className="text-xs text-muted-foreground opacity-50"></div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-card border border-border rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px]">
      <div className="divide-y divide-border">
        <div>{renderPair(teamA)}</div>
        <div>{renderPair(teamB)}</div>
      </div>
    </div>
  );
}