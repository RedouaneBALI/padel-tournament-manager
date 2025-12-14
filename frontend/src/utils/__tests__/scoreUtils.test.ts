import { initializeScoresFromScore, processSuperTieBreakScore } from '../scoreUtils';
import { Score } from '@/src/types/score';

describe('scoreUtils', () => {
  describe('initializeScoresFromScore', () => {
    it('should return empty arrays for undefined score', () => {
      const result = initializeScoresFromScore(undefined);
      expect(result).toEqual([['', '', ''], ['', '', '']]);
    });

    it('should initialize scores from normal sets', () => {
      const score: Score = {
        sets: [
          { teamAScore: 6, teamBScore: 4 },
          { teamAScore: 7, teamBScore: 5 },
          { teamAScore: 0, teamBScore: 0 },
        ],
      };
      const result = initializeScoresFromScore(score);
      expect(result).toEqual([['6', '7', '0'], ['4', '5', '0']]);
    });

    it('should handle tie-break in sets[2]', () => {
      const score: Score = {
        sets: [
          { teamAScore: 6, teamBScore: 4 },
          { teamAScore: 7, teamBScore: 5 },
          { teamAScore: 0, teamBScore: 0, tieBreakTeamA: 10, tieBreakTeamB: 8 },
        ],
      };
      const result = initializeScoresFromScore(score);
      expect(result).toEqual([['6', '7', '10'], ['4', '5', '8']]);
    });

    it('should handle tie-break at root level', () => {
      const score: Score = {
        sets: [
          { teamAScore: 6, teamBScore: 4 },
          { teamAScore: 7, teamBScore: 5 },
          { teamAScore: 0, teamBScore: 0 },
        ],
        tieBreakPointA: 10,
        tieBreakPointB: 8,
      };
      const result = initializeScoresFromScore(score);
      expect(result).toEqual([['6', '7', '10'], ['4', '5', '8']]);
    });
  });

  describe('processSuperTieBreakScore', () => {
    it('should process super tie-break correctly', () => {
      const score: Score = {
        sets: [
          { teamAScore: 6, teamBScore: 4 },
          { teamAScore: 7, teamBScore: 5 },
          { teamAScore: 0, teamBScore: 0, tieBreakTeamA: 10, tieBreakTeamB: 8 },
        ],
      };
      const result = processSuperTieBreakScore(score);
      expect(result.sets?.[2].teamAScore).toBe(10);
      expect(result.sets?.[2].teamBScore).toBe(8);
      expect(result.tieBreakPointA).toBe(10);
      expect(result.tieBreakPointB).toBe(8);
    });

    it('should not modify if not super tie-break', () => {
      const score: Score = {
        sets: [
          { teamAScore: 6, teamBScore: 4 },
          { teamAScore: 7, teamBScore: 5 },
        ],
      };
      const result = processSuperTieBreakScore(score);
      expect(result).toEqual(score);
    });
  });
});
