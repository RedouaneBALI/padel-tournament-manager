'use client';

import React, { useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';
import { normalizeGroup, groupBadgeClasses, formatGroupLabel } from '@/src/utils/groupBadge';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';
import { Stage } from '@/src/types/stage';
import { initializeScoresFromScore } from '@/src/utils/scoreUtils';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  score?: Score;
  winnerSide?: number;
  pool?: { name?: string };
  finished?: boolean;
  matchIndex?: number;
  totalMatches?: number;
  stage?: string | Stage;
  scheduledTime?: string;
  setsToWin?: number;
}

/**
 * A lightweight, read-only component for displaying match results.
 * Supports displaying scores, winners, and adapts to tournament rules like setsToWin.
 */
export default function MatchResultCardLight({ teamA, teamB, score, winnerSide, pool, finished = true, matchIndex, totalMatches, stage, scheduledTime, setsToWin }: Props) {
  const [scores] = useState<string[][]>(() => initializeScoresFromScore(score));

  // Informations d'abandon (forfeit) si présentes
  const isForfeit = !!score?.forfeit;
  const forfeitedBy = score?.forfeitedBy || null;

  // Déterminer combien de sets afficher : basé sur setsToWin et les scores existants
  const countNonEmpty = (arr: string[]) => arr.filter((s) => s !== '' && s !== undefined && s !== null).length;
  const stw = setsToWin ?? 2;
  const visibleSets = Math.min(3, Math.max(stw, countNonEmpty(scores[0]), countNonEmpty(scores[1])));

  const group = normalizeGroup(pool?.name);
  const isInProgress = !finished && (score?.sets?.some(set => set.teamAScore || set.teamBScore) || false);
  const notStarted = !finished && !isInProgress;
  // determine if this round is the final
  const isFinalStage = (() => {
    try {
      const stageStr = String(stage || '').toLowerCase();
      return stage === Stage.FINAL || stageStr === 'finale' || stageStr === 'final' || stageStr.includes('final');
    } catch (e) { return false; }
  })();


  return (
    <div className={`relative w-full bg-card border border-gray-300 rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px] transition-all duration-200`}>
      {/* Indicateur match en cours */}
      {isInProgress && (
        <div className="absolute top-2 right-2 z-10">
          <LiveMatchIndicator showLabel={false} />
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
            showChampion={finished && isFinalStage && winnerSide !== undefined && winnerSide === 0}
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
            showChampion={finished && isFinalStage && winnerSide !== undefined && winnerSide === 1}
            visibleSets={visibleSets}
            forfeited={isForfeit && forfeitedBy === 'TEAM_B'}
            showAbSlot={isForfeit}
          />
        </div>
      </div>
      {notStarted && scheduledTime && (
        <div className="absolute bottom-2 right-2 text-xs text-muted-foreground bg-background/80 px-1 py-0.5 rounded">
          {scheduledTime}
        </div>
      )}
    </div>
  );
}