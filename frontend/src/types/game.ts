'use client';

import { PlayerPair } from '@/types/playerPair';
import { Score } from '@/types/score';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onScoreSaved: (result: { tournamentUpdated: boolean; winner: String | null }) => void;
  winnerSide?: number;
  stage?: string;
}
