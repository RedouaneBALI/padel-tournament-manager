'use client';

import React from 'react';
import { Round } from '@/src/types/round';
import { stageLabels } from '@/src/types/stage';

interface Props {
  rounds: Round[];
  currentIndex: number;
  onChange: (index: number) => void;
  onStageChange?: (stage: string) => void;
}

export default function RoundSelector({ rounds, currentIndex, onChange, onStageChange }: Props) {
  const currentStage = rounds[currentIndex]?.stage;

  return (
    <div className="flex items-center justify-center gap-4">
      <button
        onClick={() => {
          const nextIndex = Math.max(currentIndex - 1, 0);
          onChange(nextIndex);
          const stage = rounds[nextIndex]?.stage;
          if (stage) onStageChange?.(stage);
        }}
        disabled={currentIndex === 0}
        className="px-2 py-1 rounded bg-muted hover:bg-muted-foreground disabled:opacity-30"
      >
        ←
      </button>

      <select
        value={currentStage}
        onChange={(e) => {
          const index = rounds.findIndex((r) => r.stage === e.target.value);
          if (index !== -1) {
            onChange(index);
            onStageChange?.(rounds[index].stage);
          }
        }}
        className="text-center text-sm px-3 py-1 border border-border rounded text-foreground"
      >
        {rounds.map((round) => (
          <option key={round.id} value={round.stage}>
            {stageLabels[round.stage]}
          </option>
        ))}
      </select>

      <button
        onClick={() => {
          const nextIndex = Math.min(currentIndex + 1, rounds.length - 1);
          onChange(nextIndex);
          const stage = rounds[nextIndex]?.stage;
          if (stage) onStageChange?.(stage);
        }}
        disabled={currentIndex === rounds.length - 1}
        className="px-2 py-1 rounded bg-muted hover:bg-muted-foreground disabled:opacity-30"
      >
        →
      </button>
    </div>
  );
}