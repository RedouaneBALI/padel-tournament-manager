import React, { useEffect, useState } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import GameAssignmentBloc from '@/src/components/admin/GameAssignmentBloc';

interface Props {
  tournament: Tournament;
  playerPairs: PlayerPair[];
}

export default function KnockoutPlayerAssignment({ tournament, playerPairs }: Props) {
  // Local, fixed-size slots array (PlayerPair | null)
  const [slots, setSlots] = useState<Array<PlayerPair | null>>([]);
  const [dragIndex, setDragIndex] = useState<number | null>(null);

  const onDragStart = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    setDragIndex(index);
    // For Firefox compatibility
    e.dataTransfer.setData('text/plain', String(index));
    e.dataTransfer.effectAllowed = 'move';
  };

  const onDragOver = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.preventDefault(); // allow drop
    e.dataTransfer.dropEffect = 'move';
  };

  const onDrop = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.preventDefault();
    const from = dragIndex ?? parseInt(e.dataTransfer.getData('text/plain') || '-1', 10);
    if (from === -1) return;

    setSlots((prev) => {
      const next = prev.slice();
      const moved = next[from] ?? null;
      if (moved == null) return prev; // nothing to move
      if (from === index) return prev;
      const dest = next[index];
      next[from] = dest ?? null; // swap if dest occupied, else leave null
      next[index] = moved;
      return next;
    });
    setDragIndex(null);
  };

  const onKeyReorder = (index: number, dir: -1 | 1) => () => {
    const target = index + dir;
    if (target < 0 || target >= slots.length) return;
    setSlots((prev) => {
      const next = prev.slice();
      const moved = next[index] ?? null;
      const dest = next[target] ?? null;
      next[index] = dest;
      next[target] = moved;
      return next;
    });
  };

  const matchIndexOf = (rowIndex: number) => Math.floor(rowIndex / 2) + 1;

  const mainDrawSize = (tournament as any)?.config?.mainDrawSize ?? (playerPairs?.length || 0);
  const matchesCount = Math.max(1, Math.floor((mainDrawSize || 0) / 2));

  const suffixOf = (p?: PlayerPair) => {
    if (!p) return '';
    if (p.seed != null) return ` (TS ${p.seed})`;
    if (p.displaySeed) {
      return p.displaySeed.startsWith('(') ? ` ${p.displaySeed}` : ` (${p.displaySeed})`;
    }
    return '';
  };

  // Build a fixed-size slots array so empty positions are real drop targets
  useEffect(() => {
    const size = matchesCount * 2;
    const next: Array<PlayerPair | null> = new Array(size).fill(null);
    (playerPairs ?? []).forEach((p, i) => {
      if (i < size) next[i] = p;
    });
    setSlots(next);
  }, [tournament, playerPairs]);

  return (
    <div className="min-h-[200px]">
      <div className="flex items-center">
        <div className="h-px flex-1 bg-border my-2" />
          <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">{`R${matchesCount}`}</h3>
        <div className="h-px flex-1 bg-border" />
      </div>

      <p className="text-sm text-tab-inactive mb-4">Faites glisser les équipes pour les ordonner manuellement.</p>

      <div className="rounded-md border border-border bg-card divide-y">
        {Array.from({ length: matchesCount }).map((_, m) => {
          const iA = m * 2;
          const iB = iA + 1;
          const pairA = slots[iA];
          const pairB = slots[iB];
          return (
            <GameAssignmentBloc
              key={`match-${m}`}
              title={`Match ${m + 1}/${matchesCount}`}
              pairA={pairA}
              pairB={pairB}
              handlersA={{
                onDragStart: pairA ? onDragStart(iA) : undefined,
                onDragOver: onDragOver(iA),
                onDrop: onDrop(iA),
                onMoveUp: onKeyReorder(iA, -1),
                onMoveDown: onKeyReorder(iA, 1),
              }}
              handlersB={{
                onDragStart: pairB ? onDragStart(iB) : undefined,
                onDragOver: onDragOver(iB),
                onDrop: onDrop(iB),
                onMoveUp: onKeyReorder(iB, -1),
                onMoveDown: onKeyReorder(iB, 1),
              }}
            />
          );
        })}
      </div>
    </div>
  );
}