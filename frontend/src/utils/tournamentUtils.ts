import { Tournament } from '@/src/types/tournament';

/**
 * Returns true if at least one game in the tournament has started (set played, points, or winner).
 */
export function hasTournamentStarted(tournament: Tournament): boolean {
  return !!tournament.rounds?.some(round =>
    round.games?.some(game => {
      if (!game.score) return false;
      // At least one set played
      if (Array.isArray(game.score.sets) && game.score.sets.length > 0) return true;
      // Points in progress
      if (game.score.currentGamePointA !== null || game.score.currentGamePointB !== null) return true;
      // Tie-break in progress
      if (game.score.tieBreakPointA !== null || game.score.tieBreakPointB !== null) return true;
      // Winner decided
      if (game.winnerSide !== null) return true;
      // Match terminé
      if (game.finished === true) return true;
      // Forfait
      if (game.score.forfeit === true) return true;
      return false;
    })
  );
}
