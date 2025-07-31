'use client';

import React, { useState, useRef } from 'react';
import { PlayerPair } from '@/types/playerPair';
import { Score } from '@/types/score';
import TeamScoreRow from '@/src/components/match/TeamScoreRow';
import { Edit3, Save, X } from 'lucide-react';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onScoreSaved: (result: { tournamentUpdated: boolean; winner: PlayerPair | null }) => void;
}

export default function MatchResultCard({ teamA, teamB, gameId, score, tournamentId, onScoreSaved }: Props) {
  const [scores, setScores] = useState<string[][]>(() => {
    const initialScores: string[][] = [[], []];
    for (let i = 0; i < 3; i++) {
      initialScores[0][i] = score?.sets[i]?.teamAScore?.toString() || '';
      initialScores[1][i] = score?.sets[i]?.teamBScore?.toString() || '';
    }
    return initialScores;
  });

  // Refs : un tableau par équipe, contenant refs inputs sets
  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array.from({ length: 2 }, () => Array(3).fill(null))
  );

  return (
    <div className={`relative bg-card border border-border rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px] transition-all duration-200`}>
      <div className="relative">
        <div className={`divide-y divide-border`}>
          <TeamScoreRow
            team={teamA}
            teamIndex={0}
            scores={scores[0]}
            setScores={(newScores) => setScores((prev) => [newScores, prev[1]])}
            inputRefs={{ current: inputRefs.current[0] }}
          />
          <TeamScoreRow
            team={teamB}
            teamIndex={1}
            scores={scores[1]}
            setScores={(newScores) => setScores((prev) => [prev[0], newScores])}
            inputRefs={{ current: inputRefs.current[1] }}
          />
        </div>
      </div>
    </div>
  );
}