import { Game } from '@/types/game';

export interface Round {
  id?: number;
  name: string;
  matches: Game[];
}