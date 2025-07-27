import { Player } from '@/src/types/player';

export interface PlayerPair {
  id?: number;
  seed?: number;
  player1: Player;
  player2: Player;
}