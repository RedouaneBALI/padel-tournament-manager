import React, { useEffect, useState } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import GameAssignmentBloc from '@/src/components/admin/GameAssignmentBloc';
import { fetchGamesByStage } from '@/src/api/tournamentApi';

interface Props {
  tournament: Tournament;
  playerPairs: PlayerPair[];
}

export default function KnockoutPlayerAssignment({ tournament, playerPairs }: Props) {
  const [slots, setSlots] = useState<Array<PlayerPair | null>>([]);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const matchRefs = React.useRef<Array<HTMLDivElement | null>>([]);
  const scrollRef = React.useRef<HTMLDivElement | null>(null);
  const autoScrollRAF = React.useRef<number | null>(null);
  const autoScrollDir = React.useRef<0 | 1 | -1>(0);

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

  // Determine the first non-GROUPS round's stage name for display
  const rounds = (tournament as any)?.rounds as Array<any> | undefined;
  let firstRoundStageName = `R${matchesCount}`;
  if (Array.isArray(rounds) && rounds.length > 0) {
    const firstBracket = rounds.find((r) => r?.stage && r.stage !== 'GROUPS');
    if (firstBracket && firstBracket.stage) {
      firstRoundStageName = firstBracket.stage;
    }
  }

  // Build a fixed-size slots array so empty positions are real drop targets.
  // Prefer the saved order from the tournament's first bracket round (teamA/teamB per game),
  // fall back to the raw playerPairs list otherwise.
  useEffect(() => {
    const size = matchesCount * 2;

    // Try to derive slots from the first non-GROUPS round
    let derived: Array<PlayerPair | null> | null = null;
    if (Array.isArray(rounds) && rounds.length > 0) {
      const firstBracket = rounds.find((r) => r?.stage && r.stage !== 'GROUPS');
      const games: Array<any> = firstBracket?.games || [];
      if (games.length > 0) {
        const next: Array<PlayerPair | null> = new Array(size).fill(null);
        for (let m = 0; m < Math.min(games.length, size / 2); m++) {
          const g = games[m];
          // Keep BYE objects as-is so they remain draggable; null remains null.
          next[m * 2] = (g?.teamA ?? null) as PlayerPair | null;
          next[m * 2 + 1] = (g?.teamB ?? null) as PlayerPair | null;
        }
        derived = next;
      }
    }

    if (derived) {
      setSlots(derived);
      return;
    }

    // Fallback: seed from the flat playerPairs list
    const next: Array<PlayerPair | null> = new Array(size).fill(null);
    (playerPairs ?? []).forEach((p, i) => {
      if (i < size) next[i] = p;
    });
    setSlots(next);
  }, [tournament, playerPairs, matchesCount, rounds]);

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

  const cancelAutoScroll = () => {
    if (autoScrollRAF.current != null) {
      cancelAnimationFrame(autoScrollRAF.current);
      autoScrollRAF.current = null;
    }
    autoScrollDir.current = 0;
  };

  const startAutoScroll = (dy: number) => {
    const container = scrollRef.current;
    const step = () => {
      if (container && container.scrollHeight > container.clientHeight) {
        container.scrollTop += dy;
      } else {
        window.scrollBy(0, dy);
      }
      autoScrollRAF.current = requestAnimationFrame(step);
    };
    autoScrollRAF.current = requestAnimationFrame(step);
  };

  const onRootDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const container = scrollRef.current;

    // Wider trigger zone and faster min speed for easier activation
    const threshold = 180;  // px near top/bottom to trigger scroll
    const maxSpeed = 28;    // px per frame (capped)

    const rect = container ? container.getBoundingClientRect() : { top: 0, bottom: window.innerHeight } as any;
    const y = e.clientY;
    const distTop = y - rect.top;
    const distBottom = rect.bottom - y;

    const speedFromDist = (d: number) => {
      if (d > threshold) return 0;
      const ratio = (threshold - d) / threshold; // 0..1
      return Math.max(6, Math.floor(ratio * maxSpeed));
    };

    const up = speedFromDist(distTop);
    const down = speedFromDist(distBottom);

    let dir: 0 | 1 | -1 = 0;
    let speed = 0;
    if (up > 0) { dir = -1; speed = up; }
    else if (down > 0) { dir = 1; speed = down; }

    if (dir === 0) {
      // Leave the zone
      cancelAutoScroll();
      return;
    }

    // If direction changed, restart the RAF with the new speed
    const desiredDy = dir * speed;
    const currentDir = autoScrollDir.current;

    if (autoScrollRAF.current == null || currentDir !== dir) {
      cancelAutoScroll();
      autoScrollDir.current = dir;
      startAutoScroll(desiredDy);
    }
  };

  const onRootDragEnd = () => {
    cancelAutoScroll();
  };

  return (
    <div
      ref={scrollRef}
      className="min-h-[200px] max-h-[80vh] overflow-auto"
      onDragOver={onRootDragOver}
      onDrop={onRootDragEnd}
      onDragEnd={onRootDragEnd}
    >
      <p className="text-sm text-center text-tab-inactive m-2">Faites glisser les équipes pour les ordonner.</p>

      <div className="flex items-center">
        <div className="h-px flex-1 bg-border my-2" />
          <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">{firstRoundStageName}</h3>
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