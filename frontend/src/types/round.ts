import { Game } from '@/src/types/game';

export interface Round {
  id?: number;
  name: string;
  matches: Game[];
}