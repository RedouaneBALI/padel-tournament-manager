import { describe, it, expect } from '@jest/globals';

/**
 * Calculate the top percentage for a player's ranking
 * Only returns a value if the player is in the top 50%
 */
const getTopPercentage = (ranking: number, totalPlayers: number): number | null => {
  if (totalPlayers === 0) return null;
  const percentage = Math.ceil((ranking / totalPlayers) * 100);
  return percentage <= 50 ? percentage : null;
};

/**
 * Get badge style classes based on the top percentage
 */
const getBadgeStyle = (percentage: number): string => {
  if (percentage <= 1) return 'gold';
  else if (percentage <= 5) return 'silver';
  else if (percentage <= 10) return 'bronze';
  else if (percentage <= 20) return 'green';
  else if (percentage <= 30) return 'cyan';
  else if (percentage <= 40) return 'blue';
  else if (percentage <= 50) return 'purple';
  return 'default';
};

describe('PlayerDetailModal - getTopPercentage', () => {
  describe.each([
    // [ranking, totalPlayers, expected]
    [1, 2153, 1],           // #1/2153 = 0.046% => top 1%
    [10, 2153, 1],          // #10/2153 = 0.46% => top 1%
    [22, 2153, 2],          // #22/2153 = 1.02% => top 2%
    [50, 2153, 3],          // #50/2153 = 2.32% => top 3%
    [100, 2153, 5],         // #100/2153 = 4.64% => top 5%
    [500, 2153, 24],        // #500/2153 = 23.22% => top 24%
    [1000, 2153, 47],       // #1000/2153 = 46.45% => top 47%
    [1076, 2153, 50],       // #1076/2153 = 49.97% => top 50%
    [1077, 2153, null],     // #1077/2153 = 50.02% => null (not in top 50%)
    [1500, 2153, null],     // #1500/2153 = 69.67% => null
    [2153, 2153, null],     // #2153/2153 = 100% => null
    [1, 100, 1],            // #1/100 = 1% => top 1%
    [50, 100, 50],          // #50/100 = 50% => top 50%
    [51, 100, null],        // #51/100 = 51% => null
    [1, 0, null],           // Edge case: no players
  ])('getTopPercentage(%i, %i)', (ranking, totalPlayers, expected) => {
    it(`should return ${expected}`, () => {
      expect(getTopPercentage(ranking, totalPlayers)).toBe(expected);
    });
  });
});

describe('PlayerDetailModal - getBadgeStyle', () => {
  describe.each([
    // [percentage, expectedColor]
    [1, 'gold'],
    [2, 'silver'],
    [5, 'silver'],
    [6, 'bronze'],
    [10, 'bronze'],
    [11, 'green'],
    [20, 'green'],
    [21, 'cyan'],
    [30, 'cyan'],
    [31, 'blue'],
    [40, 'blue'],
    [41, 'purple'],
    [50, 'purple'],
  ])('getBadgeStyle(%i)', (percentage, expectedColor) => {
    it(`should return ${expectedColor} style`, () => {
      expect(getBadgeStyle(percentage)).toBe(expectedColor);
    });
  });
});

