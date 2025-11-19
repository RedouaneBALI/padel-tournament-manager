import { useCallback, useRef, useEffect } from 'react';

// Constants for auto-scroll behavior
export const SCROLL_CONFIG = {
  THRESHOLD: 180,     // px near top/bottom to trigger scroll
  MAX_SPEED: 28,      // px per frame (capped)
  MIN_SPEED: 6,       // minimum scroll speed
} as const;

/**
 * Hook to manage auto-scroll during drag operations
 * @returns Object with auto-scroll utilities
 */
export function useAutoScroll(scrollRef: React.RefObject<HTMLDivElement | null>) {
  const autoScrollRAF = useRef<number | null>(null);
  const autoScrollDir = useRef<0 | 1 | -1>(0);

  // Cancel auto-scroll
  const cancelAutoScroll = useCallback(() => {
    if (autoScrollRAF.current != null) {
      cancelAnimationFrame(autoScrollRAF.current);
      autoScrollRAF.current = null;
    }
    autoScrollDir.current = 0;
  }, []);

  // Start auto-scroll with given speed
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
  }, [scrollRef]);

  // Calculate scroll speed based on distance from edge
  const calculateScrollSpeed = useCallback((distanceFromEdge: number): number => {
    if (distanceFromEdge > SCROLL_CONFIG.THRESHOLD) return 0;
    const ratio = (SCROLL_CONFIG.THRESHOLD - distanceFromEdge) / SCROLL_CONFIG.THRESHOLD;
    return Math.max(SCROLL_CONFIG.MIN_SPEED, Math.floor(ratio * SCROLL_CONFIG.MAX_SPEED));
  }, []);

  // Root-level drag handler for auto-scroll
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
  }, [calculateScrollSpeed, cancelAutoScroll, startAutoScroll, scrollRef]);

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
    onRootDragOver,
    onRootDragEnd,
    cancelAutoScroll,
  };
}

