import { hasTournamentStarted } from '../tournamentUtils';
import { Tournament } from '@/src/types/tournament';

describe('hasTournamentStarted', () => {
  const baseGame = {
    id: 1,
    teamA: {},
    teamB: {},
    finished: false,
    score: {
      sets: [],
      forfeit: false,
      forfeitedBy: null,
      currentGamePointA: null,
      currentGamePointB: null,
      tieBreakPointA: null,
      tieBreakPointB: null,
    },
    winnerSide: null,
    scheduledTime: '',
    court: '',
    isEditable: null,
  };

  const makeTournament = (games: any[]) => ({ rounds: [{ games }] }) as Tournament;

  it.each`
    desc                                 | gameModifications                                 | expected
    ${'aucun match'}                     | ${{}}                                              | ${false}
    ${'match vierge'}                    | ${{}}                                              | ${false}
    ${'set joué'}                        | ${{ score: { sets: [{ a: 6, b: 4 }] } }}           | ${true}
    ${'points en cours'}                 | ${{ score: { currentGamePointA: 15 } }}            | ${true}
    ${'tie-break en cours'}              | ${{ score: { tieBreakPointA: 3 } }}                | ${true}
    ${'vainqueur défini'}                | ${{ winnerSide: 'TEAM_A' }}                               | ${true}
    ${'match terminé'}                   | ${{ finished: true }}                              | ${true}
    ${'forfait'}                         | ${{ score: { forfeit: true } }}                    | ${true}
  `('$desc', ({ gameModifications, expected }) => {
    const game = { ...baseGame, ...gameModifications, score: { ...baseGame.score, ...gameModifications.score } };
    const tournament = makeTournament([game]);
    expect(hasTournamentStarted(tournament)).toBe(expected);
  });

  it('retourne false si tous les jeux sont vierges', () => {
    const games = [0, 1, 2].map(i => ({ ...baseGame, id: i }));
    const tournament = makeTournament(games);
    expect(hasTournamentStarted(tournament)).toBe(false);
  });

  it('retourne true si au moins un match a commencé', () => {
    const games = [
      { ...baseGame },
      { ...baseGame, score: { ...baseGame.score, sets: [{ a: 6, b: 4 }] } },
    ];
    const tournament = makeTournament(games);
    expect(hasTournamentStarted(tournament)).toBe(true);
  });
});
