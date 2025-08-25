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
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const matchRefs = React.useRef<Array<HTMLDivElement | null>>([]);

  const onDragStart = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    setDragIndex(index);
    setHoveredIndex(null);
    // For Firefox compatibility
    e.dataTransfer.setData('text/plain', String(index));
    e.dataTransfer.effectAllowed = 'move';
  };

  const onDragOver = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.preventDefault(); // allow drop
    e.dataTransfer.dropEffect = 'move';
    setHoveredIndex(index);
  };

  const performDrop = (index: number, e: React.DragEvent) => {
    e.preventDefault();
    const from = dragIndex ?? parseInt(e.dataTransfer.getData('text/plain') || '-1', 10);
    if (from === -1) return;

    setSlots((prev) => {
      if (from === index) return prev;
      const next = prev.slice();
      const tmp = next[index] ?? null;
      next[index] = next[from] ?? null;
      next[from] = tmp;
      return next;
    });
    setDragIndex(null);
    setHoveredIndex(null);
  };

  const onDrop = (index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.stopPropagation();
    performDrop(index, e);
  };

  const onMatchDragOver = (m: number) => (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    const el = matchRefs.current[m];
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    const iA = m * 2;
    const iB = iA + 1;
    setHoveredIndex(e.clientY <= midY ? iA : iB);
  };

  const onMatchDrop = (m: number) => (e: React.DragEvent<HTMLDivElement>) => {
    e.stopPropagation();
    const el = matchRefs.current[m];
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    const iA = m * 2;
    const iB = iA + 1;
    const targetIndex = e.clientY <= midY ? iA : iB;
    performDrop(targetIndex, e);
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

  // Build a fixed-size slots array so empty positions are real drop targets
  useEffect(() => {
    const size = matchesCount * 2;
    const next: Array<PlayerPair | null> = new Array(size).fill(null);
    (playerPairs ?? []).forEach((p, i) => {
      if (i < size) next[i] = p;
    });
    setSlots(next);
  }, [tournament, playerPairs, matchesCount]);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('knockout:pairs-reordered', {
        detail: {
          tournamentId: (tournament as any)?.id,
          slots,
        },
      }));
    }
  }, [slots, tournament]);

  return (
    <div className="min-h-[200px]">
      <p className="text-sm text-center text-tab-inactive m-2">Faites glisser les équipes pour les ordonner.</p>

      <div className="flex items-center">
        <div className="h-px flex-1 bg-border my-2" />
          <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">{`R${matchesCount}`}</h3>
        <div className="h-px flex-1 bg-border" />
      </div>


      <div className="rounded-md border border-border bg-card divide-y">
        {Array.from({ length: matchesCount }).map((_, m) => {
          const iA = m * 2;
          const iB = iA + 1;
          const pairA = slots[iA];
          const pairB = slots[iB];
          return (
            <div
              key={`match-${m}`}
              ref={(el) => (matchRefs.current[m] = el)}
              onDragOver={onMatchDragOver(m)}
              onDrop={onMatchDrop(m)}
              className="relative"
            >
              <GameAssignmentBloc
                title={`Match ${m + 1}/${matchesCount}`}
                pairA={pairA}
                pairB={pairB}
                handlersA={{
                  onDragStart: onDragStart(iA),
                  onDragOver: onDragOver(iA),
                  onDrop: onDrop(iA),
                  onMoveUp: onKeyReorder(iA, -1),
                  onMoveDown: onKeyReorder(iA, 1)
                }}
                handlersB={{
                  onDragStart: onDragStart(iB),
                  onDragOver: onDragOver(iB),
                  onDrop: onDrop(iB),
                  onMoveUp: onKeyReorder(iB, -1),
                  onMoveDown: onKeyReorder(iB, 1)
                }}
                isActiveA={dragIndex === iA}
                isActiveB={dragIndex === iB}
                isOverA={hoveredIndex === iA}
                isOverB={hoveredIndex === iB}
              />
            </div>
          );
        })}
      </div>
    </div>
  );
}