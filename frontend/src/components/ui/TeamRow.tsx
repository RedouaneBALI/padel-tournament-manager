//src/components/ui/TeamRow.tsx
'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';

interface Props {
  team: PlayerPair | null;
  winnerSide?: number;
  teamIndex?: number;
}

export default function TeamRow({ team, winnerSide, teamIndex }: Props) {
  return (
    <div
      className={`flex flex-1 items-center ${
        winnerSide !== undefined && winnerSide === teamIndex ? 'font-bold' : ''
      }`}
    >
      <div className={`flex flex-col ${winnerSide !== undefined && winnerSide !== teamIndex ? 'text-muted-foreground' : ''}`}>
        <span className="text-sm">{team?.player1Name || ''}</span>
        <span className="text-sm">{team?.player2Name || ''}</span>
      </div>

      {team?.displaySeed && (
        <span className="text-xs text-muted-foreground font-medium self-center px-2">
          ({team.displaySeed})
        </span>
      )}
    </div>
  );
}
