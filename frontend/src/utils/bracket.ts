import type { Round } from '@/src/types/round';

export function calculateMatchPositions(rounds: Round[], hideBye: boolean = false) {
  if (rounds.length === 0) return [];
  const positions: number[][] = [];

  rounds.forEach((round, roundIndex) => {
    const roundPositions: number[] = [];
    const nbMatches = round.games.length;

    if (roundIndex === 0) {
      const baseSpacing = 140;
      let currentY = 0;

      for (let i = 0; i < nbMatches; i++) {
        const game = round.games[i];
        const isBye = game.teamA?.type === 'BYE' || game.teamB?.type === 'BYE';

        // Si c'est un BYE et qu'on le masque, on assigne la position actuelle
        // mais on n'incrémente pas 'currentY', donc le prochain match visible prendra cette place.
        if (hideBye && isBye) {
          roundPositions.push(currentY);
        } else {
          roundPositions.push(currentY);
          currentY += baseSpacing;
        }
      }
    } else {
      const prevPos = positions[roundIndex - 1];
      const prevRound = rounds[roundIndex - 1];

      for (let i = 0; i < nbMatches; i++) {
        const childIdxA = i * 2;
        const childIdxB = i * 2 + 1;

        const p1 = prevPos[childIdxA] || 0;
        const p2 = prevPos[childIdxB] || 0;

        // Calcul par défaut (centré)
        let pos = (p1 + p2) / 2;

        // Si mode compact, on aligne le parent avec l'enfant visible si l'autre est un BYE
        if (hideBye) {
          const gameA = prevRound.games[childIdxA];
          const gameB = prevRound.games[childIdxB];
          const isByeA = gameA?.teamA?.type === 'BYE' || gameA?.teamB?.type === 'BYE';
          const isByeB = gameB?.teamA?.type === 'BYE' || gameB?.teamB?.type === 'BYE';

          if (isByeA && !isByeB) {
            pos = p2; // Aligner avec B (A est BYE)
          } else if (!isByeA && isByeB) {
            pos = p1; // Aligner avec A (B est BYE)
          }
        }

        roundPositions.push(pos);
      }
    }
    positions.push(roundPositions);
  });

  return positions;
}