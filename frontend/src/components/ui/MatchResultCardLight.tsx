'use client';

import React, { useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';
import { normalizeGroup, groupBadgeClasses, formatGroupLabel } from '@/src/utils/groupBadge';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  score?: Score;
  winnerSide?: number;
  pool?: { name?: string };
}

export default function MatchResultCardLight({ teamA, teamB, score, winnerSide, pool }: Props) {
  const [scores] = useState<string[][]>(() => [
    Array.from({ length: 3 }, (_, i) => score?.sets[i]?.teamAScore?.toString() || ''),
    Array.from({ length: 3 }, (_, i) => score?.sets[i]?.teamBScore?.toString() || ''),
  ]);

  const group = normalizeGroup(pool?.name);

  return (
    <div className={`relative bg-card border border-border rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px] transition-all duration-200`}>
      {pool?.name && (
        <div className="px-2 pt-2">
          <div className={['inline-block text-xs font-medium rounded px-3 py-0.5', groupBadgeClasses(group)].join(' ')}>
            {formatGroupLabel(pool.name)}
          </div>
        </div>
      )}
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