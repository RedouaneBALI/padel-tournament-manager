import MatchResultCardLight from '@/src/components/ui/MatchResultCardLight';
import type { Round } from '@/src/types/round';

interface KnockoutBracketProps {
  rounds: Round[];
  tournamentId: string;
}

export default function KnockoutBracket({ rounds, tournamentId }: KnockoutBracketProps) {
  const ROUND_WIDTH = 320;
  const calculateMatchPositions = (rounds: Round[]) => {
    if (rounds.length === 0) return [];

    const positions: number[][] = [];

    rounds.forEach((round, roundIndex) => {
      const roundPositions: number[] = [];
      const nbMatches = round.games.length;

      if (roundIndex === 0) {
        const baseSpacing = 160;
        for (let i = 0; i < nbMatches; i++) {
          roundPositions.push(i * baseSpacing);
        }
      } else {
        const previousPositions = positions[roundIndex - 1];
        for (let i = 0; i < nbMatches; i++) {
          const pos1 = previousPositions[i * 2] || 0;
          const pos2 = previousPositions[i * 2 + 1] || 0;
          roundPositions.push((pos1 + pos2) / 2);
        }
      }

      positions.push(roundPositions);
    });

    return positions;
  };

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
              />
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
