import React from 'react';
import { Score } from '@/src/types/score';
import { initializeScoresFromScore } from '@/src/utils/scoreUtils';

export function useScoreSyncing(
  score: Score | undefined,
  editing: boolean,
  setScores: (scores: string[][]) => void,
  setInitialScores: (scores: string[][]) => void,
  setIsForfeit: (isForfeit: boolean) => void,
  setForfeitedBy: (forfeitedBy: 'TEAM_A' | 'TEAM_B' | null) => void
) {
  const prevScoreSerializedRef = React.useRef<string | null>(null);

  React.useEffect(() => {
    const serialized = JSON.stringify(score || null);
    if (editing) {
      prevScoreSerializedRef.current = serialized;
      return;
    }
    if (prevScoreSerializedRef.current === serialized) return;
    prevScoreSerializedRef.current = serialized;
    const newScores = initializeScoresFromScore(score);
    setScores(newScores);
    setInitialScores(newScores.map(arr => [...arr]));
    setIsForfeit(score?.forfeit || false);
    setForfeitedBy(score?.forfeitedBy || null);
  }, [score, editing, setScores, setInitialScores, setIsForfeit, setForfeitedBy]);
}
