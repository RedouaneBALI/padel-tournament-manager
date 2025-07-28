'use client';

import React from 'react';
import { PlayerPair } from '@/types/playerPair';

interface Props {
  team: PlayerPair | null;
  teamIndex: number;
  scores: string[];
  editing: boolean;
  setScores: (scores: string[]) => void;
  inputRefs: React.MutableRefObject<(HTMLInputElement | null)[]>;
  handleKeyDown: (e: React.KeyboardEvent, teamIndex: number, setIndex: number) => void;
}

export default function TeamScoreRow({
  team,
  teamIndex,
  scores,
  editing,
  setScores,
  inputRefs,
  handleKeyDown,
}: Props) {
  const handleChange = (setIndex: number, value: string) => {
    const updated = [...scores];
    updated[setIndex] = value;
    setScores(updated);
  };

  return (
    <div className="flex items-center px-4 h-[60px]">
      <div className="flex flex-1 items-center justify-between">
        <div className="flex flex-col">
          <span className="text-sm">{team?.player1?.name || ''}</span>
          <span className="text-sm">{team?.player2?.name || ''}</span>
        </div>

        {team?.seed && (
          <span className="text-xs text-muted-foreground font-medium self-center">
            ({team.seed})
          </span>
        )}
      </div>

      <div className="text-center ml-4">
        {editing ? (
          <div className="flex space-x-1">
            {scores.map((setScore, setIndex) => (
              <input
                key={setIndex}
                type="text"
                value={setScore}
                ref={(el) => (inputRefs.current[setIndex] = el)}
                onChange={(e) => handleChange(setIndex, e.target.value)}
                onKeyDown={(e) => handleKeyDown(e, teamIndex, setIndex)}
                className="w-8 text-xs text-center border border-gray-300 rounded"
                placeholder="-"
              />
            ))}
          </div>
        ) : (
          <div className="flex space-x-2 text-base font-semibold text-gray-800 dark:text-gray-100">
            {scores.filter(Boolean).length > 0 ? (
              scores.map((s, i) => <span key={i}>{s}</span>)
            ) : (
              <span className="text-gray-400">-</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}