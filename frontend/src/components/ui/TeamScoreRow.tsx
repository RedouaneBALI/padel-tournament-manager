'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';

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
}: Props) {
  const handleChange = (setIndex: number, value: string) => {
    const updated = [...scores];
    updated[setIndex] = value;
    setScores?.(updated);
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
                type="text"
                value={setScore}
                ref={(el) => {
                  if (inputRefs) inputRefs.current[setIndex] = el;
                }}
                onChange={(e) => handleChange(setIndex, e.target.value)}
                onKeyDown={(e) => handleKeyDown?.(e, teamIndex, setIndex)}
                className="w-8 text-xs text-center border border-border rounded"
                placeholder="-"
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