import { useEffect, useRef, useState, useCallback } from 'react';

interface UseBracketZoomOptions {
  bracketWidth: number;
  bracketHeight: number;
}

export function useBracketZoom(
  containerRef: React.RefObject<HTMLDivElement>,
  options: UseBracketZoomOptions
) {
  const { bracketWidth, bracketHeight } = options;
  const [scale, setScale] = useState(1);
  const lastDistance = useRef<number | null>(null);
  const initialScale = useRef(1);
  const scaleChangeCallbacks = useRef<Set<() => void>>(new Set());
  const [minScale, setMinScale] = useState(1);

  const onScaleChange = useCallback((callback: () => void) => {
    scaleChangeCallbacks.current.add(callback);
    return () => {
      scaleChangeCallbacks.current.delete(callback);
    };
  }, []);

  // Calculate minimum scale needed to fit entire bracket
  useEffect(() => {
    const calculateMinScale = () => {
      const container = containerRef.current;
      if (!container || !bracketWidth || !bracketHeight) {
        setMinScale(1);
        return;
      }

      const containerRect = container.getBoundingClientRect();

      // Use viewport height minus offset for header/tabs (approximation)
      // This is more reliable than containerHeight which can be unlimited with overflow-auto
      const availableHeight = window.innerHeight - containerRect.top - 80; // 80px for bottom nav/padding

      // Check if bracket overflows vertically
      // We don't check width overflow - horizontal scroll is acceptable
      const overflowsHeight = bracketHeight > availableHeight;

      // If bracket fits vertically, don't allow zoom out
      if (!overflowsHeight) {
        setMinScale(1);
        return;
      }

      // Calculate scale needed to fit height only
      // Horizontal scroll is acceptable, we only care about fitting vertically
      const scaleForHeight = availableHeight / bracketHeight;

      // Use scale for height, with a floor of 0.1
      const finalMinScale = Math.max(0.1, scaleForHeight);

      setMinScale(finalMinScale);
    };

    calculateMinScale();

    // Recalculate on window resize
    window.addEventListener('resize', calculateMinScale);
    return () => window.removeEventListener('resize', calculateMinScale);
  }, [containerRef, bracketWidth, bracketHeight]);

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
        const delta = -e.deltaY * 0.003;
        setScale((prev) => Math.min(Math.max(minScale, prev + delta), 1));
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
        setScale(Math.min(Math.max(minScale, newScale), 1));
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
  }, [containerRef, scale, minScale]);

  return { scale, onScaleChange, minScale };
}

function getDistance(touch1: Touch, touch2: Touch): number {
  const dx = touch1.clientX - touch2.clientX;
  const dy = touch1.clientY - touch2.clientY;
  return Math.sqrt(dx * dx + dy * dy);
}

