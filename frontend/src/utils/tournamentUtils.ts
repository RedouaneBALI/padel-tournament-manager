import { Tournament } from '@/src/types/tournament';

export function hasTournamentStarted(tournament: Tournament): boolean {
  return !!tournament.rounds?.some(round => round.games?.some(game => game.score !== null));
}
