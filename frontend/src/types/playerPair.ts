import { PairType } from '@/src/types/pairType';

export interface PlayerPair {
  id?: number;
  seed?: number;
  player1Name: string;
  player2Name: string;
  type: PairType;
  displaySeed: string;
}