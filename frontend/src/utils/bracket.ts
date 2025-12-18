import type { Round } from '@/src/types/round';

export function calculateMatchPositions(rounds: Round[]) {
  if (rounds.length === 0) return [];
  const positions: number[][] = [];

  rounds.forEach((round, roundIndex) => {
    const roundPositions: number[] = [];
    const nbMatches = round.games.length;

    if (roundIndex === 0) {
      const baseSpacing = 140;
      for (let i = 0; i < nbMatches; i++) roundPositions.push(i * baseSpacing);
    } else {
      const prev = positions[roundIndex - 1];
      for (let i = 0; i < nbMatches; i++) {
        const p1 = prev[i * 2] || 0;
        const p2 = prev[i * 2 + 1] || 0;
        roundPositions.push((p1 + p2) / 2);
      }
    }
    positions.push(roundPositions);
  });

  return positions;
}