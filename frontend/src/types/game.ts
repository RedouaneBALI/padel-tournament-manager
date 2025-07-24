import { PlayerPair } from '@/src/types/playerPair';

export interface Game {
  id?: number;
  playerPair1: PlayerPair;
  playerPair2: PlayerPair;
  winner?: PlayerPair;
}