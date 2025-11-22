import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import { Pool } from '@/src/types/pool';

/**
 * Game data type matching the backend GameDTO.
 * This is a pure data type - UI-specific props should be kept in component interfaces.
 */
export interface Game {
  id: number | string;
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  finished: boolean;
  score?: Score;
  winnerSide?: string;
  scheduledTime?: string;
  court?: string;
  pool?: Pool;
}
