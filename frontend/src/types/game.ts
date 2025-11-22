'use client';

import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import { Pool } from '@/src/types/pool';
import { Round } from '@/src/types/round';

export interface Game {
  id: string;
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onScoreSaved: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  winnerSide?: string;
  stage?: string;
  pool?: Pool;
  finished: boolean;
  scheduledTime: string;
  court: string;
  round?: Round;
}
