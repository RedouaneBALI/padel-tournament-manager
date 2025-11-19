import { useState, useCallback, useRef, useEffect } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import { reorderPlayerPairs } from '@/src/api/tournamentApi';
import { useAutoScroll } from '@/src/hooks/useAutoScroll';

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

  // Use shared auto-scroll hook
  const { onRootDragOver, onRootDragEnd } = useAutoScroll(scrollRef);

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

  // Listen to external requests to replace main slots (e.g. apply BYE positions to main draw)
  useEffect(() => {
    const handler = (e: Event) => {
      try {
        const ce = e as CustomEvent;
        const detail = ce.detail;
        if (!detail || !Array.isArray(detail.mainSlots)) return;
        const incoming = detail.mainSlots as Array<PlayerPair | null>;
        if (incoming.length !== mainSlotsSize) return;
        setMainSlots(incoming);

        // Dispatch updated event
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('qualifko:pairs-reordered', {
            detail: {
              tournamentId: (tournament as any)?.id,
              qualifSlots,
              mainSlots: incoming,
            },
          }));
        }
      } catch (err) {
        // ignore
      }
    };
    if (typeof window !== 'undefined') {
      window.addEventListener('qualifko:apply-main-byes', handler as EventListener);
    }
    return () => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('qualifko:apply-main-byes', handler as EventListener);
      }
    };
  }, [mainSlotsSize, qualifSlots, tournament]);


  // Move item from one list to another
  const moveBetweenLists = useCallback((fromList: 'qualif' | 'main', fromIndex: number, toList: 'qualif' | 'main', toIndex: number) => {
    const persistOrder = async (newQualif: Array<PlayerPair | null>, newMain: Array<PlayerPair | null>) => {
      // Concaténer les paires non nulles dans l'ordre qualif puis main
      const orderedPairs = [...newQualif, ...newMain].filter(Boolean) as PlayerPair[];
      // Extraire uniquement les IDs des paires
      const pairIds = orderedPairs.map(pair => pair.id).filter((id): id is number => id !== undefined);
      try {
        await reorderPlayerPairs((tournament as any)?.id, pairIds);
      } catch (e) {
        // Optionnel : afficher une erreur ou logger
        // toast.error("Erreur lors de la sauvegarde de l'ordre des paires");
      }
    };

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
      // Sauvegarder l'ordre
      persistOrder(fromList === 'qualif' ? newSlots : qualifSlots, fromList === 'main' ? newSlots : mainSlots);
    } else {
      // Different lists, swap
      const fromSetter = fromList === 'qualif' ? setQualifSlots : setMainSlots;
      const toSetter = toList === 'qualif' ? setQualifSlots : setMainSlots;
      const fromGetter = fromList === 'qualif' ? qualifSlots : mainSlots;
      const toGetter = toList === 'qualif' ? qualifSlots : mainSlots;

      const itemFrom = fromGetter[fromIndex];
      const itemTo = toGetter[toIndex];

      const newFrom = [...fromGetter];
      const newTo = [...toGetter];
      newFrom[fromIndex] = itemTo;
      newTo[toIndex] = itemFrom;

      fromSetter(newFrom);
      toSetter(newTo);

      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('qualifko:pairs-reordered', {
          detail: {
            tournamentId: (tournament as any)?.id,
            qualifSlots: toList === 'qualif' ? newTo : newFrom,
            mainSlots: toList === 'main' ? newTo : newFrom,
          },
        }));
      }
      // Sauvegarder l'ordre
      persistOrder(toList === 'qualif' ? newTo : newFrom, toList === 'main' ? newTo : newFrom);
    }
  }, [qualifSlots, mainSlots, tournament]);

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

  // Ajout de la sauvegarde explicite lors d'une action utilisateur (drag & drop effectif)
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