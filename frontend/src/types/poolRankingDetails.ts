import { PlayerPair } from '@/src/types/playerPair';

export interface PoolRankingDetails {
  id?: number;
  playerPair: PlayerPair;
  points: number;
  setAverage: number;
}
