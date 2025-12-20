'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import TeamRow from '@/src/components/ui/TeamRow';
import { Flag } from 'lucide-react';

function focusByTabIndex(nextIndex: number) {
  const el = document.querySelector<HTMLInputElement>(`input[tabindex="${nextIndex}"]`);
  if (el) {
    el.focus();
    el.select?.();
  }
}

interface Props {
  team: PlayerPair | null;
  teamIndex: number;
  scores: string[];
  editing: boolean;
  setScores?: (scores: string[]) => void;
  inputRefs?: React.MutableRefObject<(HTMLInputElement | null)[]>;
  handleKeyDown?: (e: React.KeyboardEvent, teamIndex: number, setIndex: number) => void;
  winnerSide?: number;
  visibleSets?: number;
  computeTabIndex?: (teamIndex: number, setIndex: number) => number;
  forfeited?: boolean;
  showAbSlot?: boolean;
  onToggleForfeit?: () => void;
  showChampion?: boolean;
  hideScores?: boolean;
}

export default function TeamScoreRow({
  team,
  teamIndex,
  scores,
  editing,
  setScores,
  inputRefs,
  handleKeyDown,
  winnerSide,
  visibleSets,
  computeTabIndex,
  forfeited,
  showAbSlot,
  onToggleForfeit,
  showChampion,
  hideScores,
}: Props) {
  const handleChange = (setIndex: number, value: string) => {
    const sanitized = value.replace(/[^0-9]/g, '');
    const updated = [...scores];
    updated[setIndex] = sanitized;
    setScores?.(updated);
  };

  const onKeyDownLocal = (e: React.KeyboardEvent, tIdx: number, sIdx: number) => {
    handleKeyDown?.(e, tIdx, sIdx);
  };

  const setsCount = visibleSets ?? scores.length;

  const scoresCols = `repeat(${setsCount}, var(--team-score-cell-width-rem)` + ')';
  const gapVar = 'var(--team-score-cell-gap-rem)';
  const abSlot = 'var(--team-score-ab-slot-width-rem)';
  const gridTemplate = showAbSlot
    ? `${scoresCols} ${gapVar} ${abSlot} ${gapVar}`
    : `${scoresCols}`;

  const isWinner = winnerSide !== undefined && winnerSide === teamIndex;
  const isBye = team?.type === 'BYE';

  return (
    <div className={`relative flex items-center pl-4 pr-2 h-[60px] ${isWinner ? 'winner-highlight' : ''} ${isBye ? 'bye-highlight' : ''}`}>
      <div className="flex-1 min-w-0">
        <TeamRow team={team} winnerSide={winnerSide} teamIndex={teamIndex} showChampion={showChampion} />
      </div>

      {editing ? (
        <div className="flex items-center gap-2 ml-4">
          <button
            type="button"
            onClick={onToggleForfeit}
            className={`flex items-center justify-center rounded border border-border text-xs font-bold transition-colors ${forfeited ? 'bg-yellow-500/20 text-yellow-700 border-yellow-500/30' : 'bg-muted/30 text-muted-foreground'}`}
            title="Abandon"
          >
            <Flag className="h-2 w-2" />
            <span className={forfeited ? 'text-[10px] font-bold' : 'text-[10px] font-normal text-muted-foreground'}>AB</span>
          </button>
          <div className="flex space-x-1">
            {scores.slice(0, setsCount).map((setScore, setIndex) => (
              <input
                key={setIndex}
                type="tel"
                inputMode="numeric"
                pattern="[0-9]*"
                value={setScore}
                ref={(el) => {
                  if (inputRefs) inputRefs.current[setIndex] = el;
                }}
                onChange={(e) => handleChange(setIndex, e.target.value)}
                onKeyDown={(e) => onKeyDownLocal(e, teamIndex, setIndex)}
                className="w-8 text-xs text-center border border-border rounded tabular-nums"
                placeholder="-"
                tabIndex={computeTabIndex ? computeTabIndex(teamIndex, setIndex) : undefined}
                enterKeyHint="next"
              />
            ))}
          </div>
        </div>
      ) : (
        <div
          className="ml-auto team-score-grid"
          style={{ gridTemplateColumns: gridTemplate }}
        >
          {hideScores ? (
            Array.from({ length: setsCount }).map((_, i) => (
              <div key={`score-${i}`} className="text-center tabular-nums text-base leading-none font-semibold team-score-justify-center">
                {/* Empty space where scores would be */}
              </div>
            ))
          ) : (
            Array.from({ length: setsCount }).map((_, i) => {
              let displayValue = scores[i] || '';
              if (
                i === 2 &&
                team &&
                (team as any).scores &&
                (team as any).scores.length > 2 &&
                ((teamIndex === 0 && (team as any).scores[2].tieBreakTeamA != null) ||
                  (teamIndex === 1 && (team as any).scores[2].tieBreakTeamB != null))
              ) {
                displayValue = teamIndex === 0
                  ? (team as any).scores[2].tieBreakTeamA?.toString() || ''
                  : (team as any).scores[2].tieBreakTeamB?.toString() || '';
              }
              return (
                <div key={`score-${i}`} className={`text-center tabular-nums text-base leading-none ${isWinner ? 'font-bold' : 'font-semibold'} team-score-justify-center`}>
                  {displayValue}
                </div>
              );
            })
          )}

          {showAbSlot && (
            <div key="ab-slot" className="flex items-center justify-center" style={{ width: `var(--team-score-ab-slot-width-rem)` }}>
              {forfeited ? (
                <span className="inline-flex items-center justify-center h-5 text-[10px] font-bold px-1 py-0.5 rounded bg-yellow-500/20 text-yellow-700 border border-yellow-500/30 leading-none">
                  AB
                </span>
              ) : (
                <span className="inline-flex items-center justify-center h-5 px-1 py-0.5 rounded border border-transparent bg-transparent text-transparent leading-none">&nbsp;</span>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
