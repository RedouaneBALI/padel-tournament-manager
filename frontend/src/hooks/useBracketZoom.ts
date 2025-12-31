import { useEffect, useRef, useState, useCallback } from 'react';

export function useBracketZoom(containerRef: React.RefObject<HTMLDivElement>) {
  const [scale, setScale] = useState(1);
  const lastDistance = useRef<number | null>(null);
  const initialScale = useRef(1);
  const scaleChangeCallbacks = useRef<Set<() => void>>(new Set());

  const onScaleChange = useCallback((callback: () => void) => {
    scaleChangeCallbacks.current.add(callback);
    return () => {
      scaleChangeCallbacks.current.delete(callback);
    };
  }, []);

  useEffect(() => {
    // Notify all listeners when scale changes
    scaleChangeCallbacks.current.forEach(cb => cb());
  }, [scale]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    // Handle wheel zoom (desktop)
    const handleWheel = (e: WheelEvent) => {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault();
        const delta = -e.deltaY * 0.003; // Increased from 0.001 for faster zoom
        setScale((prev) => Math.min(Math.max(0.25, prev + delta), 1));
      }
    };

    // Handle touch zoom (mobile)
    const handleTouchStart = (e: TouchEvent) => {
      if (e.touches.length === 2) {
        lastDistance.current = getDistance(e.touches[0], e.touches[1]);
        initialScale.current = scale;
      }
    };

    const handleTouchMove = (e: TouchEvent) => {
      if (e.touches.length === 2 && lastDistance.current !== null) {
        e.preventDefault();
        const currentDistance = getDistance(e.touches[0], e.touches[1]);
        const scaleChange = currentDistance / lastDistance.current;
        const newScale = initialScale.current * scaleChange;
        setScale(Math.min(Math.max(0.25, newScale), 1)); // Max scale 1 (100%), min scale 0.25 (25%)
      }
    };

    const handleTouchEnd = () => {
      lastDistance.current = null;
    };

    container.addEventListener('wheel', handleWheel, { passive: false });
    container.addEventListener('touchstart', handleTouchStart, { passive: true });
    container.addEventListener('touchmove', handleTouchMove, { passive: false });
    container.addEventListener('touchend', handleTouchEnd);

    return () => {
      container.removeEventListener('wheel', handleWheel);
      container.removeEventListener('touchstart', handleTouchStart);
      container.removeEventListener('touchmove', handleTouchMove);
      container.removeEventListener('touchend', handleTouchEnd);
    };
  }, [containerRef, scale]);

  return { scale, onScaleChange };
}

function getDistance(touch1: Touch, touch2: Touch): number {
  const dx = touch1.clientX - touch2.clientX;
  const dy = touch1.clientY - touch2.clientY;
  return Math.sqrt(dx * dx + dy * dy);
}

