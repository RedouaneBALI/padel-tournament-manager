import { useState, useCallback, useRef, useEffect } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import { reorderPlayerPairs } from '@/src/api/tournamentApi';

// Constants for auto-scroll behavior
const SCROLL_CONFIG = {
  THRESHOLD: 180,     // px near top/bottom to trigger scroll
  MAX_SPEED: 28,      // px per frame (capped)
  MIN_SPEED: 6,       // minimum scroll speed
} as const;

export function usePlayerAssignment(tournament: Tournament, playerPairs: PlayerPair[], slotsSize: number) {
  const [slots, setSlots] = useState<Array<PlayerPair | null>>([]);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const matchRefs = useRef<Array<HTMLDivElement | null>>([]);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const autoScrollRAF = useRef<number | null>(null);
  const autoScrollDir = useRef<0 | 1 | -1>(0);

  // Initialize slots from tournament data or playerPairs
  useEffect(() => {
    const rounds = (tournament as any)?.rounds as Array<any> | undefined;
    let derived: Array<PlayerPair | null> | null = null;

    // Try to derive slots from the first non-GROUPS round
    if (Array.isArray(rounds) && rounds.length > 0) {
      const firstBracket = rounds.find((r) => r?.stage && r.stage !== 'GROUPS');
      const games: Array<any> = firstBracket?.games || [];

      // Check if at least one game has actual team data
      const hasTeamData = games.some((g) => g?.teamA || g?.teamB);

      if (hasTeamData && games.length > 0) {
        const next: Array<PlayerPair | null> = new Array(slotsSize).fill(null);
        const maxGames = Math.min(games.length, slotsSize / 2);

        for (let m = 0; m < maxGames; m++) {
          const g = games[m];
          // Keep BYE objects as-is so they remain draggable; null remains null
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
    const next: Array<PlayerPair | null> = new Array(slotsSize).fill(null);
    playerPairs.forEach((p, i) => {
      if (i < slotsSize) next[i] = p;
    });
    setSlots(next);
  }, [tournament.id, slotsSize, playerPairs]);

  // Dispatch custom event when slots change
  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('knockout:pairs-reordered', {
        detail: {
          tournamentId: (tournament as any)?.id,
          slots,
        },
      }));
    }
  }, [slots, tournament.id]);

  // Auto-scroll utilities
  const cancelAutoScroll = useCallback(() => {
    if (autoScrollRAF.current != null) {
      cancelAnimationFrame(autoScrollRAF.current);
      autoScrollRAF.current = null;
    }
    autoScrollDir.current = 0;
  }, []);

  const startAutoScroll = useCallback((dy: number) => {
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
  }, []);

  // Calculate scroll speed based on distance from edge
  const calculateScrollSpeed = useCallback((distanceFromEdge: number): number => {
    if (distanceFromEdge > SCROLL_CONFIG.THRESHOLD) return 0;
    const ratio = (SCROLL_CONFIG.THRESHOLD - distanceFromEdge) / SCROLL_CONFIG.THRESHOLD;
    return Math.max(SCROLL_CONFIG.MIN_SPEED, Math.floor(ratio * SCROLL_CONFIG.MAX_SPEED));
  }, []);

  // Swap two slots
  const swapSlots = useCallback((fromIndex: number, toIndex: number) => {
    if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return;

    setSlots((prev) => {
      if (fromIndex >= prev.length || toIndex >= prev.length) return prev;

      const next = [...prev];
      const temp = next[toIndex] ?? null;
      next[toIndex] = next[fromIndex] ?? null;
      next[fromIndex] = temp;

      // Sauvegarder l'ordre mis à jour
      const orderedPairs = next.filter(Boolean) as PlayerPair[];
      const pairIds = orderedPairs.map(pair => pair.id).filter((id): id is number => id !== undefined);
      reorderPlayerPairs((tournament as any)?.id, pairIds).catch((e) => {
        // Optionnel : gérer l'erreur silencieusement ou logger
      });

      return next;
    });
  }, [tournament]);

  // Drag and drop handlers
  const onDragStart = useCallback((index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    setDragIndex(index);
    setHoveredIndex(null);
    // For Firefox compatibility
    e.dataTransfer.setData('text/plain', String(index));
    e.dataTransfer.effectAllowed = 'move';
  }, []);

  const onDragOver = useCallback((index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setHoveredIndex(index);
  }, []);

  const performDrop = useCallback((targetIndex: number, e: React.DragEvent) => {
    e.preventDefault();
    const fromIndex = dragIndex ?? parseInt(e.dataTransfer.getData('text/plain') || '-1', 10);

    if (fromIndex === -1) return;

    swapSlots(fromIndex, targetIndex);
    setDragIndex(null);
    setHoveredIndex(null);
  }, [dragIndex, swapSlots]);

  const onDrop = useCallback((index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.stopPropagation();
    performDrop(index, e);
  }, [performDrop]);

  // Match-level drag handlers (drop between two slots)
  const onMatchDragOver = useCallback((matchIndex: number) => (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';

    const el = matchRefs.current[matchIndex];
    if (!el) return;

    const rect = el.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    const slotIndexA = matchIndex * 2;
    const slotIndexB = slotIndexA + 1;

    setHoveredIndex(e.clientY <= midY ? slotIndexA : slotIndexB);
  }, []);

  const onMatchDrop = useCallback((matchIndex: number) => (e: React.DragEvent<HTMLDivElement>) => {
    e.stopPropagation();

    const el = matchRefs.current[matchIndex];
    if (!el) return;

    const rect = el.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    const slotIndexA = matchIndex * 2;
    const slotIndexB = slotIndexA + 1;
    const targetIndex = e.clientY <= midY ? slotIndexA : slotIndexB;

    performDrop(targetIndex, e);
  }, [performDrop]);

  // Keyboard reordering
  const onKeyReorder = useCallback((index: number, direction: -1 | 1) => () => {
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= slots.length) return;

    swapSlots(index, targetIndex);
  }, [slots.length, swapSlots]);

  // Root-level drag handlers for auto-scroll
  const onRootDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();

    const container = scrollRef.current;
    const rect = container
      ? container.getBoundingClientRect()
      : { top: 0, bottom: window.innerHeight };

    const y = e.clientY;
    const distTop = y - rect.top;
    const distBottom = rect.bottom - y;

    const upSpeed = calculateScrollSpeed(distTop);
    const downSpeed = calculateScrollSpeed(distBottom);

    let dir: 0 | 1 | -1 = 0;
    let speed = 0;

    if (upSpeed > 0) {
      dir = -1;
      speed = upSpeed;
    } else if (downSpeed > 0) {
      dir = 1;
      speed = downSpeed;
    }

    // Not in scroll zone - cancel auto-scroll
    if (dir === 0) {
      cancelAutoScroll();
      return;
    }

    // Direction changed or no scroll active - restart with new speed
    const desiredDy = dir * speed;
    const currentDir = autoScrollDir.current;

    if (autoScrollRAF.current == null || currentDir !== dir) {
      cancelAutoScroll();
      autoScrollDir.current = dir;
      startAutoScroll(desiredDy);
    }
  }, [calculateScrollSpeed, cancelAutoScroll, startAutoScroll]);

  const onRootDragEnd = useCallback(() => {
    cancelAutoScroll();
  }, [cancelAutoScroll]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      cancelAutoScroll();
    };
  }, [cancelAutoScroll]);

  return {
    slots,
    dragIndex,
    hoveredIndex,
    matchRefs,
    scrollRef,
    onDragStart,
    onDragOver,
    onDrop,
    onMatchDragOver,
    onMatchDrop,
    onKeyReorder,
    onRootDragOver,
    onRootDragEnd,
  };
}
