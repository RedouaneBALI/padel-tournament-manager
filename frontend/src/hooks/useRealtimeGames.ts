import { useEffect, useRef } from 'react';
import { useSession } from 'next-auth/react';
import { Score } from '@/src/types/score';
import { getStompManager } from './useRealtimeGame';

interface UpdateScoreDTO {
  tournamentUpdated: boolean;
  winner: string | null;
  score: Score;
}

interface UseRealtimeGamesOptions {
  gameIds: string[];
  onScoreUpdate: (gameId: string, dto: UpdateScoreDTO) => void;
  enabled?: boolean;
}

/**
 * Hook to subscribe to real-time updates for multiple games via STOMP.
 * Useful for list views where we want to track several games at once.
 * Shares the same STOMP connection as useRealtimeGame.
 */
export function useRealtimeGames({ gameIds, onScoreUpdate, enabled = true }: UseRealtimeGamesOptions) {
  const { data: session } = useSession();
  const unsubscribersRef = useRef<Map<string, () => void>>(new Map());
  const callbackRef = useRef(onScoreUpdate);
  const previousGameIdsRef = useRef<string[]>([]);

  // Keep callback ref up to date without triggering re-subscription
  useEffect(() => {
    callbackRef.current = onScoreUpdate;
  }, [onScoreUpdate]);

  useEffect(() => {
    if (!enabled) {
      // Cleanup all subscriptions when disabled
      unsubscribersRef.current.forEach((unsub) => unsub());
      unsubscribersRef.current.clear();
      previousGameIdsRef.current = [];
      return;
    }

    const manager = getStompManager(session);
    const currentIdsSet = new Set(gameIds);
    const previousIdsSet = new Set(previousGameIdsRef.current);

    // Find IDs to unsubscribe (were in previous but not in current)
    const toUnsubscribe = previousGameIdsRef.current.filter(id => !currentIdsSet.has(id));

    // Find IDs to subscribe (are in current but not in previous)
    const toSubscribe = gameIds.filter(id => !previousIdsSet.has(id));

    // Unsubscribe from removed games
    toUnsubscribe.forEach(id => {
      const unsub = unsubscribersRef.current.get(id);
      if (unsub) {
        unsub();
        unsubscribersRef.current.delete(id);
      }
    });

    // Subscribe to new games
    toSubscribe.forEach(async (gameId) => {
      if (!gameId || unsubscribersRef.current.has(gameId)) return;

      const destination = `/topic/game/${gameId}`;
      try {
        const unsubscribe = await manager.subscribe(destination, (message) => {
          try {
            const dto: UpdateScoreDTO = JSON.parse(message.body);
            callbackRef.current(gameId, dto);
          } catch (e) {
            // Only log parsing errors
            console.error(`[useRealtimeGames] Failed to parse message for game ${gameId}:`, e);
          }
        });
        unsubscribersRef.current.set(gameId, unsubscribe);
      } catch (e) {
        console.error(`[useRealtimeGames] Subscription error for game ${gameId}:`, e);
      }
    });

    // Update previous IDs ref
    previousGameIdsRef.current = [...gameIds];

  }, [gameIds.join(','), enabled, session]);

  // Cleanup only on unmount
  useEffect(() => {
    return () => {
      unsubscribersRef.current.forEach((unsub) => unsub());
      unsubscribersRef.current.clear();
    };
  }, []);

  return { connected: true };
}
