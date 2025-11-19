import { useState, useCallback, useRef, useEffect } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import { reorderPlayerPairs } from '@/src/api/tournamentApi';
import { useAutoScroll } from '@/src/hooks/useAutoScroll';

export function usePlayerAssignment(tournament: Tournament, playerPairs: PlayerPair[], slotsSize: number) {
  const [slots, setSlots] = useState<Array<PlayerPair | null>>([]);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const matchRefs = useRef<Array<HTMLDivElement | null>>([]);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  // Use shared auto-scroll hook
  const { onRootDragOver, onRootDragEnd } = useAutoScroll(scrollRef);

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

  // Listen to external requests to replace slots (e.g. apply BYE positions)
  useEffect(() => {
    const handler = (e: Event) => {
      try {
        const ce = e as CustomEvent;
        const detail = ce.detail;
        if (!detail || !Array.isArray(detail.slots)) return;
        const incoming = detail.slots as Array<PlayerPair | null>;
        if (incoming.length !== slotsSize) return;
        setSlots(incoming);
      } catch (err) {
        // ignore
      }
    };
    if (typeof window !== 'undefined') {
      window.addEventListener('knockout:apply-byes', handler as EventListener);
    }
    return () => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('knockout:apply-byes', handler as EventListener);
      }
    };
  }, [slotsSize]);

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
