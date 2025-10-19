import { useState, useCallback, useRef, useEffect } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import { savePlayerPairs } from '@/src/api/tournamentApi';

// Constants for auto-scroll behavior
const SCROLL_CONFIG = {
  THRESHOLD: 180,     // px near top/bottom to trigger scroll
  MAX_SPEED: 28,      // px per frame (capped)
  MIN_SPEED: 6,       // minimum scroll speed
} as const;

export function useDualPlayerAssignment(
  tournament: Tournament,
  playerPairs: PlayerPair[],
  qualifSlotsSize: number,
  mainSlotsSize: number
) {
  const [qualifSlots, setQualifSlots] = useState<Array<PlayerPair | null>>([]);
  const [mainSlots, setMainSlots] = useState<Array<PlayerPair | null>>([]);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [dragList, setDragList] = useState<'qualif' | 'main' | null>(null);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [hoveredList, setHoveredList] = useState<'qualif' | 'main' | null>(null);

  const qualifRefs = useRef<Array<HTMLDivElement | null>>([]);
  const mainRefs = useRef<Array<HTMLDivElement | null>>([]);
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const autoScrollRAF = useRef<number | null>(null);
  const autoScrollDir = useRef<0 | 1 | -1>(0);

  // Initialize slots from playerPairs
  useEffect(() => {
    const nextQualif: Array<PlayerPair | null> = new Array(qualifSlotsSize).fill(null);
    const nextMain: Array<PlayerPair | null> = new Array(mainSlotsSize).fill(null);

    // Assign players in the order received
    for (let i = 0; i < playerPairs.length; i++) {
      if (i < qualifSlotsSize) {
        nextQualif[i] = playerPairs[i];
      } else if (i < qualifSlotsSize + mainSlotsSize) {
        nextMain[i - qualifSlotsSize] = playerPairs[i];
      }
    }

    setQualifSlots(nextQualif);
    setMainSlots(nextMain);

    // Dispatch initial slots
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('qualifko:pairs-reordered', {
        detail: {
          tournamentId: (tournament as any)?.id,
          qualifSlots: nextQualif,
          mainSlots: nextMain,
        },
      }));
    }
  }, [playerPairs, qualifSlotsSize, mainSlotsSize, tournament.id]);

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

  // Move item from one list to another
  const moveBetweenLists = useCallback((fromList: 'qualif' | 'main', fromIndex: number, toList: 'qualif' | 'main', toIndex: number) => {
    if (fromList === toList) {
      // Same list, swap
      const setter = fromList === 'qualif' ? setQualifSlots : setMainSlots;
      const newSlots = [...(fromList === 'qualif' ? qualifSlots : mainSlots)];
      const temp = newSlots[toIndex] ?? null;
      newSlots[toIndex] = newSlots[fromIndex] ?? null;
      newSlots[fromIndex] = temp;

      setter(newSlots);

      // Dispatch updated slots
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('qualifko:pairs-reordered', {
          detail: {
            tournamentId: (tournament as any)?.id,
            qualifSlots: fromList === 'qualif' ? newSlots : qualifSlots,
            mainSlots: fromList === 'main' ? newSlots : mainSlots,
          },
        }));
      }
    } else {
      // Different lists, swap
      const fromSetter = fromList === 'qualif' ? setQualifSlots : setMainSlots;
      const toSetter = toList === 'qualif' ? setQualifSlots : setMainSlots;
      const fromGetter = fromList === 'qualif' ? qualifSlots : mainSlots;
      const toGetter = toList === 'qualif' ? qualifSlots : mainSlots;

      const itemFrom = fromGetter[fromIndex];
      const itemTo = toGetter[toIndex];

      const newFrom = [...fromGetter];
      newFrom[fromIndex] = itemTo;
      const newTo = [...toGetter];
      newTo[toIndex] = itemFrom;

      fromSetter(newFrom);
      toSetter(newTo);

      // Dispatch updated slots
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('qualifko:pairs-reordered', {
          detail: {
            tournamentId: (tournament as any)?.id,
            qualifSlots: fromList === 'qualif' ? newFrom : newTo,
            mainSlots: fromList === 'main' ? newFrom : newTo,
          },
        }));
      }
    }
  }, [qualifSlots, mainSlots, tournament.id]);

  // Drag and drop handlers
  const onDragStart = useCallback((list: 'qualif' | 'main', index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    setDragIndex(index);
    setDragList(list);
    setHoveredIndex(null);
    setHoveredList(null);
    e.dataTransfer.setData('text/plain', `${list}-${index}`);
    e.dataTransfer.effectAllowed = 'move';
  }, []);

  const onDragOver = useCallback((list: 'qualif' | 'main', index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setHoveredIndex(index);
    setHoveredList(list);
  }, []);

  const performDrop = useCallback((targetList: 'qualif' | 'main', targetIndex: number, e: React.DragEvent) => {
    e.preventDefault();
    const data = e.dataTransfer.getData('text/plain');
    const [fromList, fromIndexStr] = data.split('-');
    const fromIndex = parseInt(fromIndexStr, 10);

    if (fromList !== 'qualif' && fromList !== 'main') return;

    moveBetweenLists(fromList, fromIndex, targetList, targetIndex);
    setDragIndex(null);
    setDragList(null);
    setHoveredIndex(null);
    setHoveredList(null);
  }, [moveBetweenLists]);

  const onDrop = useCallback((list: 'qualif' | 'main', index: number) => (e: React.DragEvent<HTMLLIElement>) => {
    e.stopPropagation();
    performDrop(list, index, e);
  }, [performDrop]);

  // Match-level drag handlers
  const onMatchDragOver = useCallback((list: 'qualif' | 'main', matchIndex: number) => (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';

    const refs = list === 'qualif' ? qualifRefs : mainRefs;
    const el = refs.current[matchIndex];
    if (!el) return;

    const rect = el.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    const slotIndexA = matchIndex * 2;
    const slotIndexB = slotIndexA + 1;

    setHoveredIndex(e.clientY <= midY ? slotIndexA : slotIndexB);
    setHoveredList(list);
  }, []);

  const onMatchDrop = useCallback((list: 'qualif' | 'main', matchIndex: number) => (e: React.DragEvent<HTMLDivElement>) => {
    e.stopPropagation();

    const refs = list === 'qualif' ? qualifRefs : mainRefs;
    const el = refs.current[matchIndex];
    if (!el) return;

    const rect = el.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    const slotIndexA = matchIndex * 2;
    const slotIndexB = slotIndexA + 1;
    const targetIndex = e.clientY <= midY ? slotIndexA : slotIndexB;

    performDrop(list, targetIndex, e);
  }, [performDrop]);

  // Keyboard reordering
  const onKeyReorder = useCallback((list: 'qualif' | 'main', index: number, direction: -1 | 1) => () => {
    const targetIndex = index + direction;
    const slots = list === 'qualif' ? qualifSlots : mainSlots;
    if (targetIndex < 0 || targetIndex >= slots.length) return;

    moveBetweenLists(list, index, list, targetIndex);
  }, [qualifSlots.length, mainSlots.length, moveBetweenLists]);

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

  // Ajout d'un effet pour sauvegarder à chaque drag & drop
  useEffect(() => {
    const allPairs: PlayerPair[] = [...qualifSlots, ...mainSlots].filter((p): p is PlayerPair => !!p);
    // On sauvegarde l'ordre même si certains slots sont vides
    if ((qualifSlots.length > 0 || mainSlots.length > 0) && allPairs.length > 0) {
      savePlayerPairs((tournament as any)?.id, allPairs);
    }
  }, [qualifSlots, mainSlots, tournament]);

  return {
    qualifSlots,
    mainSlots,
    dragIndex,
    dragList,
    hoveredIndex,
    hoveredList,
    qualifRefs,
    mainRefs,
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
