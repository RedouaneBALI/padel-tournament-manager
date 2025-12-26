import type { Round } from '@/src/types/round';

function calculateFirstRoundPositions(round: Round, hideBye: boolean): number[] {
  const roundPositions: number[] = [];
  const baseSpacing = 140;
  let currentY = 0;

  for (let i = 0; i < round.games.length; i++) {
    const game = round.games[i];
    const isBye = game.teamA?.type === 'BYE' || game.teamB?.type === 'BYE';

    if (hideBye && isBye) {
      roundPositions.push(currentY);
    } else {
      roundPositions.push(currentY);
      currentY += baseSpacing;
    }
  }

  return roundPositions;
}

function calculatePositionForMatch(i: number, prevPos: number[], prevRound: Round, hideBye: boolean): number {
  const childIdxA = i * 2;
  const childIdxB = i * 2 + 1;

  const p1 = prevPos[childIdxA] || 0;
  const p2 = prevPos[childIdxB] || 0;

  let pos = (p1 + p2) / 2;

  if (hideBye) {
    const gameA = prevRound.games[childIdxA];
    const gameB = prevRound.games[childIdxB];
    const isByeA = gameA?.teamA?.type === 'BYE' || gameA?.teamB?.type === 'BYE';
    const isByeB = gameB?.teamA?.type === 'BYE' || gameB?.teamB?.type === 'BYE';

    if (isByeA && !isByeB) {
      pos = p2;
    } else if (!isByeA && isByeB) {
      pos = p1;
    }
  }

  return pos;
}

export function calculateMatchPositions(rounds: Round[], hideBye: boolean = false) {
  if (rounds.length === 0) return [];
  const positions: number[][] = [];

  rounds.forEach((round, roundIndex) => {
    if (roundIndex === 0) {
      positions.push(calculateFirstRoundPositions(round, hideBye));
    } else {
      const prevPos = positions[roundIndex - 1];
      const prevRound = rounds[roundIndex - 1];
      const roundPositions = round.games.map((_, i) => calculatePositionForMatch(i, prevPos, prevRound, hideBye));
      positions.push(roundPositions);
    }
  });

  return positions;
}