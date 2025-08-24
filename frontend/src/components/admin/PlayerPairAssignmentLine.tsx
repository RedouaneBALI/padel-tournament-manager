import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';

interface LineProps {
  pair: PlayerPair | null;
  onDragStart?: (e: React.DragEvent<HTMLLIElement>) => void;
  onDragOver: (e: React.DragEvent<HTMLLIElement>) => void;
  onDrop: (e: React.DragEvent<HTMLLIElement>) => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}

export default function PlayerPairAssignmentLine({ pair, onDragStart, onDragOver, onDrop, onMoveUp, onMoveDown }: LineProps) {
  return (
    <li
      className="flex items-center gap-3 px-3 py-2 rounded select-none cursor-grab active:cursor-grabbing border border-transparent hover:border-border"
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
          <div className="flex items-center justify-between gap-3 w-full">
            <div className="flex flex-col leading-tight">
              <span className="text-sm font-medium">{pair.player1Name}</span>
              {pair.player2Name ? <span className="text-sm">{pair.player2Name}</span> : null}
            </div>
            {pair?.displaySeed ? (
              <span className="text-xs text-muted-foreground font-medium self-center px-2">({pair.displaySeed})</span>
            ) : (
              <span className="text-xs text-muted-foreground font-medium self-center px-2" />
            )}
          </div>
        ) : (
          <div className="flex items-center justify-between gap-3 w-full">
            <div className="text-xs text-muted-foreground italic">BYE</div>
            <span className="text-xs text-muted-foreground font-medium self-center px-2" />
          </div>
        )}
      </div>
      <div className="shrink-0 text-muted-foreground" aria-hidden>⋮⋮</div>
    </li>
  );
}