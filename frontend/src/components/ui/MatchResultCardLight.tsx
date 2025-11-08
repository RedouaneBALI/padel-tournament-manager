'use client';

import React, { useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';
import { normalizeGroup, groupBadgeClasses, formatGroupLabel } from '@/src/utils/groupBadge';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  score?: Score;
  winnerSide?: number;
  pool?: { name?: string };
  finished?: boolean;
  matchIndex?: number;
  totalMatches?: number;
}

export default function MatchResultCardLight({ teamA, teamB, score, winnerSide, pool, finished = true, matchIndex, totalMatches }: Props) {
  const [scores] = useState<string[][]>(() => [
    Array.from({ length: 3 }, (_, i) => score?.sets[i]?.teamAScore?.toString() || ''),
    Array.from({ length: 3 }, (_, i) => score?.sets[i]?.teamBScore?.toString() || ''),
  ]);

  // Informations d'abandon (forfeit) si présentes
  const isForfeit = !!score?.forfeit;
  const forfeitedBy = score?.forfeitedBy || null;

  // Déterminer combien de sets afficher : nombre maximal de sets non vides entre les deux équipes
  const countNonEmpty = (arr: string[]) => arr.filter((s) => s !== '' && s !== undefined && s !== null).length;
  const visibleSets = Math.max(2, countNonEmpty(scores[0]), countNonEmpty(scores[1]));

  const group = normalizeGroup(pool?.name);
  const isInProgress = !finished && (score?.sets?.some(set => set.teamAScore || set.teamBScore) || false);


  return (
    <div className={`relative w-full bg-card border border-gray-300 rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px] transition-all duration-200`}>
      {/* Indicateur match en cours */}
      {isInProgress && (
        <div className="absolute top-2 right-2 z-10">
          <LiveMatchIndicator />
        </div>
      )}

      {pool?.name && (
        <div className="px-2 pt-2">
          <div className={['inline-block text-xs font-medium rounded px-3 py-0.5', groupBadgeClasses(group)].join(' ')}>
            {formatGroupLabel(pool.name)}
          </div>
        </div>
      )}
      {/* Badge de numéro du match si pas de pool */}
      {(!pool?.name && matchIndex !== undefined && totalMatches !== undefined) && (
        <div className="px-2 pt-2">
          <div className={['inline-block text-xs font-medium rounded px-3 py-0.5 bg-border text-foreground'].join(' ')}>
            {`${matchIndex + 1}/${totalMatches}`}
          </div>
        </div>
      )}
      <div className="relative">
        <div className={`divide-y divide-gray-200`}>
          <TeamScoreRow
            team={teamA}
            teamIndex={0}
            scores={scores[0]}
            editing={false}
            winnerSide={winnerSide}
            visibleSets={visibleSets}
            forfeited={isForfeit && forfeitedBy === 'TEAM_A'}
            showAbSlot={isForfeit}
          />
          <TeamScoreRow
            team={teamB}
            teamIndex={1}
            scores={scores[1]}
            editing={false}
            winnerSide={winnerSide}
            visibleSets={visibleSets}
            forfeited={isForfeit && forfeitedBy === 'TEAM_B'}
            showAbSlot={isForfeit}
          />
        </div>
      </div>
    </div>
  );
}