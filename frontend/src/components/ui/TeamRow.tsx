//src/components/ui/TeamRow.tsx
'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Trophy } from 'lucide-react';

interface Props {
  team: PlayerPair | null;
  winnerSide?: number;
  teamIndex?: number;
  showChampion?: boolean;
}

export default function TeamRow({ team, winnerSide, teamIndex, showChampion }: Props) {
  return (
    <div
      className={`flex flex-1 items-center gap-2 ${
        winnerSide !== undefined && winnerSide === teamIndex ? 'font-bold' : ''
      }`}
    >
      <div className={`flex flex-col ${winnerSide !== undefined && winnerSide !== teamIndex ? 'text-muted-foreground' : ''}`}>
        <span className="text-sm">{team?.player1Name || ''}</span>
        <span className="text-sm">{team?.player2Name || ''}</span>
      </div>

      {team?.displaySeed ? (
        <span className="text-xs text-muted-foreground font-medium flex items-center gap-2">
          ({team.displaySeed})
          {showChampion && (
            <span title="Vainqueur du tournoi" aria-label="Vainqueur du tournoi" role="img" className="inline-flex items-center">
              <Trophy className="h-5 w-5 text-foreground" />
            </span>
          )}
        </span>
      ) : (
        // no seed: still render a small trophy aligned like the seed when applicable
        showChampion ? (
          <span className="self-center ml-2 text-xs text-muted-foreground font-medium" title="Vainqueur du tournoi" aria-label="Vainqueur du tournoi" role="img">
            <Trophy className="h-5 w-5 text-foreground" />
          </span>
        ) : null
      )}
    </div>
  );
}