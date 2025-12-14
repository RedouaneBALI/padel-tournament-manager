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

/**
 * Converts scores array to Score object.
 * @param scores - 2D array of scores for both teams.
 * @param visibleSets - Number of visible sets.
 * @param isForfeit - Whether the match is forfeited.
 * @param forfeitedBy - Which team forfeited.
 * @returns The Score object.
 */
export function convertToScoreObject(scores: string[][], visibleSets: number, isForfeit: boolean, forfeitedBy: 'TEAM_A' | 'TEAM_B' | null): Score {
  const sets: any[] = [];
  for (let i = 0; i < visibleSets; i++) {
    const teamAScore = parseInt(scores[0][i], 10) || 0;
    const teamBScore = parseInt(scores[1][i], 10) || 0;
    sets.push({
      teamAScore,
      teamBScore,
    });
  }

  // Handle tie-break for the last set if visibleSets > 2 or something, but for now simple
  const score: Score = {
    sets,
    forfeit: isForfeit,
    forfeitedBy,
  };

  // If there's a third set and it's a tie-break, handle it
  if (visibleSets >= 3 && scores[0][2] && scores[1][2]) {
    const set3A = parseInt(scores[0][2], 10);
    const set3B = parseInt(scores[1][2], 10);
    if (set3A > 6 || set3B > 6) {
      // Assume super tie-break
      // Set tie-break points in the set object (as tieBreakTeamA/B)
      (sets[2] as any).tieBreakTeamA = set3A;
      (sets[2] as any).tieBreakTeamB = set3B;
      // Also set at root level for compatibility
      score.tieBreakPointA = set3A;
      score.tieBreakPointB = set3B;
      // Determine winner: the one with higher score wins the set 1-0
      if (set3A > set3B) {
        sets[2].teamAScore = 1;
        sets[2].teamBScore = 0;
      } else {
        sets[2].teamAScore = 0;
        sets[2].teamBScore = 1;
      }
    } else {
      sets[2].teamAScore = set3A;
      sets[2].teamBScore = set3B;
    }
  }

  return score;
}
