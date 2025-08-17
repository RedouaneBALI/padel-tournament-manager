import { Game } from '@/src/types/game';
import { Stage } from '@/src/types/stage';
import { Pool } from '@/src/types/pool';
import { MatchFormat } from '@/src/types/matchFormat';

export interface Round {
  id?: number;
  stage: Stage;
  games: Game[];
  pools : Pool[];
  matchFormat : MatchFormat;
}