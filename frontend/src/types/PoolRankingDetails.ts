import { PlayerPair } from '@/types/playerPair';

export interface PoolRankingDetails {
  id?: number;
  playerPair: PlayerPair;
  points: number;
  setAverage: number;
}
