'use client';

import React, { useState } from 'react';
import { PlayerPair } from '@/types/playerPair';
import { Score } from '@/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  score?: Score;
  winnerSide?: number;
}

export default function MatchResultCardLight({ teamA, teamB, score, winnerSide }: Props) {
  const [scores] = useState<string[][]>(() => [
    Array.from({ length: 3 }, (_, i) => score?.sets[i]?.teamAScore?.toString() || ''),
    Array.from({ length: 3 }, (_, i) => score?.sets[i]?.teamBScore?.toString() || ''),
  ]);

  return (
    <div className={`relative bg-card border border-border rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px] transition-all duration-200`}>
      <div className="relative">
        <div className={`divide-y divide-border`}>
          <TeamScoreRow
            team={teamA}
            teamIndex={0}
            scores={scores[0]}
            editing={false}
            winnerSide={winnerSide}
          />
          <TeamScoreRow
            team={teamB}
            teamIndex={1}
            scores={scores[1]}
            editing={false}
            winnerSide={winnerSide}
          />
        </div>
      </div>
    </div>
  );
}