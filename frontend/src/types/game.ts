import { PlayerPair } from '@/src/types/playerPair';
import { MatchFormat } from '@/src/types/matchFormat';

export interface Game {
  id?: number;
  teamA: PlayerPair;
  teamB: PlayerPair;
  winner?: PlayerPair;
  matchFormat: MatchFormat;
  court: string;
  scheduledTime?: string;
}