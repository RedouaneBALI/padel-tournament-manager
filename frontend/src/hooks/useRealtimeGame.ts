import { useEffect, useRef } from 'react';
import { useSession } from 'next-auth/react';
import { StompClientManager } from '@/src/lib/realtime/StompClientManager';
import { Score } from '@/src/types/score';

interface UpdateScoreDTO {
  tournamentUpdated: boolean;
  winner: string | null;
  score: Score;
}

interface UseRealtimeGameOptions {
  gameId: string;
  onScoreUpdate: (dto: UpdateScoreDTO) => void;
  enabled?: boolean;
}

// Global singleton instance
let globalStompManager: StompClientManager | null = null;

function getStompManager(session: any): StompClientManager {
  if (!globalStompManager) {
    globalStompManager = new StompClientManager({
      authTokenProvider: async () => {
        const token = session?.idToken ?? session?.accessToken;
        return token || null;
      }
    });
  }
  return globalStompManager;
}

/**
 * Hook to subscribe to real-time game updates via STOMP.
 * Replaces periodic polling with WebSocket push notifications.
 */
export function useRealtimeGame({ gameId, onScoreUpdate, enabled = true }: UseRealtimeGameOptions) {
  const { data: session } = useSession();
  const unsubscribeRef = useRef<(() => void) | null>(null);
  const callbackRef = useRef(onScoreUpdate);

  // Keep callback ref up to date without triggering re-subscription
  useEffect(() => {
    callbackRef.current = onScoreUpdate;
  }, [onScoreUpdate]);

  useEffect(() => {
    if (!enabled || !gameId) return;

    const manager = getStompManager(session);
    const destination = `/topic/game/${gameId}`;

    // Subscribe with stable callback that uses the ref
    const subscribeAsync = async () => {
      try {
        const unsubscribe = await manager.subscribe(destination, (message) => {
          try {
            const dto: UpdateScoreDTO = JSON.parse(message.body);
            callbackRef.current(dto);
          } catch (e) {
            console.error('[useRealtimeGame] Failed to parse message:', e);
          }
        });
        unsubscribeRef.current = unsubscribe;
      } catch (e) {
        console.error('[useRealtimeGame] Subscription error:', e);
      }
    };

    subscribeAsync();

    // Cleanup on unmount or gameId change
    return () => {
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
    };
  }, [gameId, enabled, session]);

  return { connected: true }; // Could be enhanced to track connection state
}

