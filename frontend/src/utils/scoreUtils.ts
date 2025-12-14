import { Score } from '@/src/types/score';

/**
 * Initializes scores array from a Score object, handling tie-breaks.
 * @param score - The Score object to initialize from, can be undefined.
 * @returns A 2D array of strings representing scores for both teams.
 */
export function initializeScoresFromScore(score: Score | undefined): string[][] {
  if (!score) return [['', '', ''], ['', '', '']];
  const initialScores: string[][] = [[], []];
  for (let i = 0; i < 3; i++) {
    if (
      i === 2 &&
      score?.sets?.[2] &&
      (score.sets[2] as any).tieBreakTeamA != null &&
      (score.sets[2] as any).tieBreakTeamB != null
    ) {
      initialScores[0][i] = (score.sets[2] as any).tieBreakTeamA.toString();
      initialScores[1][i] = (score.sets[2] as any).tieBreakTeamB.toString();
    } else if (i === 2 && score?.tieBreakPointA != null && score?.tieBreakPointB != null) {
      initialScores[0][i] = score.tieBreakPointA.toString();
      initialScores[1][i] = score.tieBreakPointB.toString();
    } else {
      initialScores[0][i] = score?.sets?.[i]?.teamAScore?.toString() || '';
      initialScores[1][i] = score?.sets?.[i]?.teamBScore?.toString() || '';
    }
  }
  return initialScores;
}

/**
 * Processes a Score object to handle super tie-break adjustments.
 * @param score - The Score object to process.
 * @param matchFormat - Optional match format for additional checks.
 * @returns The processed Score object.
 */
export function processSuperTieBreakScore(score: Score, matchFormat?: any): Score {
  let newScore = { ...score };
  const isSuperTB = (
    (matchFormat?.superTieBreakInFinalSet || (newScore.tieBreakPointA != null || newScore.tieBreakPointB != null) || (newScore.sets?.[2]?.tieBreakTeamA != null || newScore.sets?.[2]?.tieBreakTeamB != null))
    && newScore.sets?.length === 3
  );
  if (isSuperTB && newScore.sets && newScore.sets.length === 3) {
    const set3 = newScore.sets[2];
    if (typeof set3.tieBreakTeamA === 'number') {
      newScore.sets[2].teamAScore = set3.tieBreakTeamA;
      newScore.tieBreakPointA = set3.tieBreakTeamA;
    }
    if (typeof set3.tieBreakTeamB === 'number') {
      newScore.sets[2].teamBScore = set3.tieBreakTeamB;
      newScore.tieBreakPointB = set3.tieBreakTeamB;
    }
  }
  return newScore;
}
