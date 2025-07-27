import { Game } from '@/src/types/game';

export interface Round {
  id?: number;
  stage: string;
  matches: Game[];
}