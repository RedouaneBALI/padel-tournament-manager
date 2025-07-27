import { PlayerPair } from '@/src/types/playerPair';

export interface Game {
  id?: number;
  teamA: PlayerPair;
  teamB: PlayerPair;
  winner?: PlayerPair;
}