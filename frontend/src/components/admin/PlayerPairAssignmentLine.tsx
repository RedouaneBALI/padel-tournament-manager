// src/components/admin/PlayerPairAssignmentLine.tsx
import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import TeamRow from '@/src/components/ui/TeamRow';

interface LineProps {
  pair: PlayerPair | null;
  onDragStart?: (e: React.DragEvent<HTMLLIElement>) => void;
  onDragOver: (e: React.DragEvent<HTMLLIElement>) => void;
  onDrop: (e: React.DragEvent<HTMLLIElement>) => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  isActive?: boolean;
  isOver?: boolean;
}

export default function PlayerPairAssignmentLine({ pair, onDragStart, onDragOver, onDrop, onMoveUp, onMoveDown, isActive, isOver }: LineProps) {
  return (
    <li
      className={`flex items-center gap-3 px-3 py-3 rounded select-none cursor-grab active:cursor-grabbing border transition-colors ${
        isActive ? 'bg-primary/5 border-primary/40' : isOver ? 'bg-primary/10 border-primary' : 'border-transparent hover:border-border'
      }`}
      draggable={!!pair}
      onDragStart={pair ? onDragStart : undefined}
      onDragOver={onDragOver}
      onDrop={onDrop}
    >
      <button
        type="button"
        aria-label="Monter"
        onClick={onMoveUp}
        className="px-2 py-1 text-xs rounded border border-border hover:bg-background"
      >↑</button>
      <button
        type="button"
        aria-label="Descendre"
        onClick={onMoveDown}
        className="px-2 py-1 text-xs rounded border border-border hover:bg-background"
      >↓</button>
      <div className="flex-1">
        {pair ? (
          <TeamRow team={pair} winnerSide={undefined} />
        ) : (
          <div className="flex items-center justify-between gap-3 w-full">
            <div className="text-xs text-muted-foreground italic">BYE</div>
            <span className="text-xs text-muted-foreground font-medium self-center px-2" />
          </div>
        )}
      </div>
      <div className="shrink-0 text-muted-foreground px-2 py-2" aria-hidden>⋮⋮</div>
    </li>
  );
}