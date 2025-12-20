'use client';

import { Score } from '@/src/types/score';
import { Pool } from '@/src/types/pool';
import { Round } from '@/src/types/round';
import { PlayerPair } from '@/src/types/playerPair';
import { VoteSummary } from '@/src/types/vote';

export interface Game {
  id: string;
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  isEditable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onScoreSaved: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  winnerSide?: string;
  pool?: Pool;
  finished: boolean;
  scheduledTime: string;
  court: string;
  round?: Round;
  votes?: VoteSummary;
}

export type { PlayerPair } from '@/src/types/playerPair';
export type { TeamSide } from '@/src/types/teamSide';
