import { Score } from '@/src/types/score';
import { Stage } from '@/src/types/stage';
import { formatGroupLabel } from '@/src/utils/groupBadge';

export function computeVisibleSets(setsToWin: number | undefined, scores: string[][]): number {
  const setsToWinValue = setsToWin ?? 2;
  if (setsToWinValue === 1) return 1;
  if (setsToWinValue !== 2) return 3;

  const computeWins = () => {
    const firstSetA = parseInt(scores[0][0], 10);
    const firstSetB = parseInt(scores[1][0], 10);
    const secondSetA = parseInt(scores[0][1], 10);
    const secondSetB = parseInt(scores[1][1], 10);

    const firstSetValid = !isNaN(firstSetA) && !isNaN(firstSetB);
    const secondSetValid = !isNaN(secondSetA) && !isNaN(secondSetB);

    let winsA = 0;
    let winsB = 0;

    if (firstSetValid) {
      if (firstSetA > firstSetB) winsA++;
      else if (firstSetB > firstSetA) winsB++;
    }
    if (secondSetValid) {
      if (secondSetA > secondSetB) winsA++;
      else if (secondSetB > secondSetA) winsB++;
    }

    return { winsA, winsB };
  };

  const { winsA, winsB } = computeWins();
  return winsA === 1 && winsB === 1 ? 3 : 2;
}

export function computeIsInProgress(finished: boolean, score: Score | undefined, scores: string[][]): boolean {
  const propHasScores = !!(score?.sets?.some(set => set.teamAScore != null || set.teamBScore != null));
  const localHasScores = !!(scores && (scores[0].some(s => s !== '' && s != null) || scores[1].some(s => s !== '' && s != null)));
  return !finished && (propHasScores || localHasScores);
}

export function computeBadgeLabel(pool: { name?: string } | undefined, matchIndex: number | undefined, totalMatches: number | undefined): string {
  if (pool?.name) {
    return formatGroupLabel(pool.name);
  } else if (matchIndex !== undefined && totalMatches !== undefined) {
    return `${matchIndex + 1}/${totalMatches}`;
  } else {
    return '';
  }
}

export function computeShowChampion(stage: string | undefined, finished: boolean, winnerSide: number | undefined, teamIndex: number): boolean {
  try {
    const stageStr = String(stage || '').toLowerCase();
    const isFinalStage = stage === Stage.FINAL || stageStr === 'finale' || stageStr === 'final' || stageStr.includes('final');
    return finished && isFinalStage && winnerSide !== undefined && winnerSide === teamIndex;
  } catch (e) { return false; }
}
