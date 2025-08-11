'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';

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
}: Props) {
  const handleChange = (setIndex: number, value: string) => {
    const sanitized = value.replace(/[^0-9]/g, '');
    const updated = [...scores];
    updated[setIndex] = sanitized;
    setScores?.(updated);
  };

  const onKeyDownLocal = (e: React.KeyboardEvent, tIdx: number, sIdx: number) => {
    // Mobile "Next" often emits Enter/NumpadEnter
    if (e.key === 'Enter' || e.key === 'NumpadEnter') {
      if (computeTabIndex) {
        e.preventDefault();
        const current = computeTabIndex(tIdx, sIdx);
        focusByTabIndex(current + 1);
        return;
      }
    }
    handleKeyDown?.(e, tIdx, sIdx);
  };

  return (
    <div className="flex items-center px-4 h-[60px]">
      <div className={`flex flex-1 items-center ${winnerSide === teamIndex ? 'font-bold' : ''}`}>
        <div className={`flex flex-col ${winnerSide !== undefined && winnerSide !== teamIndex ? 'text-muted-foreground' : ''}`}>
          <span className="text-sm">{team?.player1?.name || ''}</span>
          <span className="text-sm">{team?.player2?.name || ''}</span>
        </div>

        {team?.seed && (
          <span className="text-xs text-muted-foreground font-medium self-center px-2">
            ({team.seed})
          </span>
        )}
      </div>

      <div className="text-center ml-4">
        {editing ? (
          <div className="flex space-x-1">
            {scores.slice(0, visibleSets ?? 3).map((setScore, setIndex) => (
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
                className="w-8 text-xs text-center border border-border rounded"
                placeholder="-"
                tabIndex={computeTabIndex ? computeTabIndex(teamIndex, setIndex) : undefined}
                enterKeyHint="next"
              />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-3 text-base font-semibold text-foreground">
            {scores.filter(Boolean).length > 0 ? (
              (() => {
                const nonEmptyScores = scores.filter(s => s !== '');
                const displayedScores = [...Array(3 - nonEmptyScores.length).fill(''), ...nonEmptyScores];
                return displayedScores.map((s, i) => (
                  <span key={i} className="text-center min-w-[2ch]">{s}</span>
                ));
              })()
            ) : (
              <span className="text-muted-foreground col-span-3 text-center">-</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}