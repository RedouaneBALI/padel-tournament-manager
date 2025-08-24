import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import PlayerPairAssignmentLine from '@/src/components/admin/PlayerPairAssignmentLine';

interface Handlers {
  onDragStart?: (e: React.DragEvent<HTMLLIElement>) => void;
  onDragOver: (e: React.DragEvent<HTMLLIElement>) => void;
  onDrop: (e: React.DragEvent<HTMLLIElement>) => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}

interface Props {
  title: string;
  pairA: PlayerPair | null;
  pairB: PlayerPair | null;
  handlersA: Handlers;
  handlersB: Handlers;
}

export default function GameAssignmentBloc({ title, pairA, pairB, handlersA, handlersB }: Props) {
  return (
    <div className="p-2">
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-sm font-semibold">{title}</h4>
        {(!pairA && !pairB) && (
          <span className="text-xs text-yellow-500" title="Match entièrement vide">⚠️</span>
        )}
      </div>
      <ul className="space-y-1">
        <PlayerPairAssignmentLine pair={pairA} {...handlersA} />
        <PlayerPairAssignmentLine pair={pairB} {...handlersB} />
      </ul>
    </div>
  );
}