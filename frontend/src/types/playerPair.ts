import { Player } from '@/src/types/player';

export interface PlayerPair {
  id?: number;
  seed?: number;
  player1Name: string;
  player2Name: string;
  type: 'NORMAL'| 'BYE' | 'QUALIFIER';
}