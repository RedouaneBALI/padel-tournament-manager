import type { Game } from '@/src/types/game';
import type { Score, GamePoint } from '@/src/types/score';
import type { MatchFormat } from '@/src/types/matchFormat';
import { initializeScoresFromScore } from '@/src/utils/scoreUtils';

export function formatGamePoint(point: GamePoint | null | undefined): string {
  if (point === null || point === undefined) return '0';
  switch (point) {
    case 'ZERO': return '0';
    case 'QUINZE': return '15';
    case 'TRENTE': return '30';
    case 'QUARANTE': return '40';
    case 'AVANTAGE': return 'AD';
    default: return '-';
  }
}

export function formatScheduledTime(value?: string | null): string | null {
  if (!value) return null;
  const trimmed = value.trim();
  if (/^\d{1,2}:\d{2}$/.test(trimmed)) return trimmed;
  const parsed = new Date(trimmed);
  if (Number.isNaN(parsed.getTime())) return trimmed;
  return parsed.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export function hasMatchStarted(score: Score | null | undefined): boolean {
  if (!score) return false;

  if (score.sets?.some(set => (set.teamAScore ?? 0) > 0 || (set.teamBScore ?? 0) > 0)) return true;
  if (score.currentGamePointA && score.currentGamePointA !== 'ZERO') return true;
  if (score.currentGamePointB && score.currentGamePointB !== 'ZERO') return true;
  if ((score.tieBreakPointA ?? 0) > 0) return true;
  if ((score.tieBreakPointB ?? 0) > 0) return true;

  return false;
}

export function processDisplayData(currentGame: Game, matchFormat: MatchFormat | null) {
  const scores = initializeScoresFromScore(currentGame.score);
  let setScoresA = scores[0].map(s => s === '' ? null : parseInt(s));
  let setScoresB = scores[1].map(s => s === '' ? null : parseInt(s));
  let tieBreakPointA = currentGame.score?.tieBreakPointA;
  let tieBreakPointB = currentGame.score?.tieBreakPointB;

  const isSuperTieBreak = (
    (matchFormat?.superTieBreakInFinalSet
      || currentGame.score?.tieBreakPointA != null
      || currentGame.score?.tieBreakPointB != null
      || currentGame.score?.sets?.[2]?.tieBreakTeamA != null
      || currentGame.score?.sets?.[2]?.tieBreakTeamB != null)
    && currentGame.score?.sets?.length === 3
  );

  const MAX_SETS = 3;
  if (isSuperTieBreak && currentGame.score?.sets?.[2]) {
    const set3 = currentGame.score.sets[2];
    setScoresA = [setScoresA[0], setScoresA[1], set3.tieBreakTeamA ?? setScoresA[2]];
    setScoresB = [setScoresB[0], setScoresB[1], set3.tieBreakTeamB ?? setScoresB[2]];

    if (tieBreakPointA == null && set3.tieBreakTeamA != null) {
      tieBreakPointA = set3.tieBreakTeamA;
    }
    if (tieBreakPointB == null && set3.tieBreakTeamB != null) {
      tieBreakPointB = set3.tieBreakTeamB;
    }
  } else {
    setScoresA = setScoresA.slice(0, 3);
    setScoresB = setScoresB.slice(0, 3);
  }

  while (setScoresA.length < MAX_SETS) setScoresA.push(null);
  while (setScoresB.length < MAX_SETS) setScoresB.push(null);

  return { setScoresA, setScoresB, tieBreakPointA, tieBreakPointB };
}

