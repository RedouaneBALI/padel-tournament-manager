import MatchResultCardLight from '@/src/components/ui/MatchResultCardLight';
import type { Round } from '@/src/types/round';
import { calculateMatchPositions } from '@/src/utils/bracket';

interface KnockoutBracketProps {
  rounds: Round[];
  tournamentId: string;
}

export default function KnockoutBracket({ rounds, tournamentId }: KnockoutBracketProps) {
  const ROUND_WIDTH = 320;
  const matchPositions = calculateMatchPositions(rounds);
  const maxPosition = Math.max(...matchPositions.flat()) + 200;

  return (
    <div
      className="relative flex"
      style={{
        width: `${rounds.length * ROUND_WIDTH}px`,
        height: `${maxPosition}px`,
      }}
    >
      {rounds.map((round, roundIndex) => (
        <div key={round.id} className="relative" style={{ width: ROUND_WIDTH }}>
          <div className="absolute top-0 left-0 right-0 text-center mb-4 text-sm font-semibold border-b-2 border-primary text-primary">
            {round.stage}
          </div>

          {round.games.map((game, gameIndex) => (
            <div
              key={game.id}
              className="absolute"
              style={{
                top: `${matchPositions[roundIndex][gameIndex] + 40}px`,
                left: '10px',
                right: '10px',
              }}
            >
              <MatchResultCardLight
                teamA={game.teamA}
                teamB={game.teamB}
                score={game.score}
                winnerSide={
                  game.finished
                    ? game.winnerSide === 'TEAM_A'
                      ? 0
                      : game.winnerSide === 'TEAM_B'
                        ? 1
                        : undefined
                    : undefined
                }
                finished={game.finished}
              />
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
