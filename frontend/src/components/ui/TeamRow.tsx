//src/components/ui/TeamRow.tsx
'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Trophy } from 'lucide-react';
import { cn } from '@/src/lib/utils';

interface Props {
  team: PlayerPair | null;
  winnerSide?: number;
  teamIndex?: number;
  showChampion?: boolean;
  fontSize?: string;
}

export default function TeamRow({ team, winnerSide, teamIndex, showChampion, fontSize }: Props) {
  return (
    <div
      className={`flex flex-1 items-center gap-2 ${
        winnerSide !== undefined && winnerSide === teamIndex ? 'font-bold' : ''
      }`}
    >
      <div className={cn('flex flex-col', fontSize ? fontSize : 'text-sm', winnerSide !== undefined && winnerSide !== teamIndex ? 'text-muted-foreground' : '')}>
        {team?.player1Name && <span>{team.player1Name}</span>}
        {team?.player2Name && <span>{team.player2Name}</span>}
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