import { Game } from '@/src/types/game';
import { Stage } from '@/src/types/stage';

export interface Round {
  id?: number;
  stage: Stage;
  games: Game[];
}