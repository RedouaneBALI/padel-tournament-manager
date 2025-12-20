import React, { useContext } from 'react';
import type { Game } from '@/src/types/game';
import type { MatchFormat } from '@/src/types/matchFormat';
import { processDisplayData } from '@/src/utils/zoomMatchUtils';
import ZoomTeamScoreRow from '@/src/components/match/ZoomTeamScoreRow';
import { formatStageLabel } from '@/src/types/stage';
import { TournamentNameContext } from '@/src/contexts/TournamentNameContext';

type MatchShareCardProps = {
  game: Game;
  tournamentName?: string;
  matchFormat?: MatchFormat | null;
};

/**
 * Shareable card for match details, optimized for Instagram.
 */
export default function MatchShareCard({
  game,
  tournamentName,
  matchFormat,
}: MatchShareCardProps) {
  // Use context as fallback only when tournamentName is not provided
  // (context won't work in temp DOM, so rely on prop for export functionality)
  const contextTournamentName = useContext(TournamentNameContext);
  const displayName = tournamentName || contextTournamentName;

  const teams: (any | null)[] = [game.teamA ?? null, game.teamB ?? null];
  const {
    setScoresA,
    setScoresB,
    tieBreakPointA,
    tieBreakPointB
  } = processDisplayData(game, matchFormat);
  const winnerSideA = game.winnerSide === 'TEAM_A' ? 0 : game.winnerSide === 'TEAM_B' ? 1 : undefined;
  const winnerSideB = winnerSideA;
  return (
    <div className="match-share-card modal-content w-full max-w-sm rounded-xl shadow-lg overflow-hidden border border-gray-200 flex flex-col">
      {/* Header: Tournament Name & Round */}
      <div className="modal-header px-4 py-3 text-center" style={{ backgroundColor: 'var(--color-primary)', color: 'var(--color-on-primary)' }}>
        {displayName && (
          <h1 className="text-lg font-bold truncate">{displayName}</h1>
        )}
        {game.round?.stage && (
          <div className="text-xs font-semibold mt-1">
            {formatStageLabel(game.round.stage)}
          </div>
        )}
      </div>

      {/* Scoreboard - Compact */}
      <div className="px-3 py-4 flex-1 flex flex-col justify-center gap-3 bg-white">
        {/* Équipe A */}
        <ZoomTeamScoreRow
          team={teams[0]}
          teamIndex={0}
          gamePoint={game.score?.currentGamePointA}
          setScores={setScoresA}
          tieBreakPoint={tieBreakPointA}
          teamSide="TEAM_A"
          editable={false}
          loading={null}
          onPointChange={() => {}}
          winnerSide={winnerSideA}
          isFinished={game.finished}
        />

        {/* Équipe B */}
        <ZoomTeamScoreRow
          team={teams[1]}
          teamIndex={1}
          gamePoint={game.score?.currentGamePointB}
          setScores={setScoresB}
          tieBreakPoint={tieBreakPointB}
          teamSide="TEAM_B"
          editable={false}
          loading={null}
          onPointChange={() => {}}
          winnerSide={winnerSideB}
          isFinished={game.finished}
        />
      </div>

      {/* Footer */}
      <div className="modal-footer px-4 py-3 text-center text-xs font-semibold tracking-wider" style={{ backgroundColor: 'var(--color-brand-instagram)', color: 'white' }}>
        www.padelrounds.com
      </div>
    </div>
  );
}
