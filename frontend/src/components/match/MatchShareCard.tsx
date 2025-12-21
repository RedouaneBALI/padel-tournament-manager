import React, { useContext } from 'react';
import type { Game } from '@/src/types/game';
import type { MatchFormat } from '@/src/types/matchFormat';
import { processDisplayData } from '@/src/utils/zoomMatchUtils';
import ZoomTeamScoreRow from '@/src/components/match/ZoomTeamScoreRow';
import { formatStageLabel } from '@/src/types/stage';
import { TournamentContext } from '@/src/contexts/TournamentContext';

export type MatchShareCardProps = {
  game: Game;
  tournamentName?: string;
  club?: string | null;
  matchFormat?: MatchFormat | null;
};

export default function MatchShareCard({
  game,
  tournamentName,
  club,
  matchFormat,
}: MatchShareCardProps) {
  const contextTournament = useContext(TournamentContext);
  const displayName = tournamentName || contextTournament?.name;
  const displayClub = club || contextTournament?.club;
  const teams: (any | null)[] = [game.teamA ?? null, game.teamB ?? null];
  const { setScoresA, setScoresB, tieBreakPointA, tieBreakPointB } = processDisplayData(game, matchFormat);
  const winnerSideA = game.winnerSide === 'TEAM_A' ? 0 : game.winnerSide === 'TEAM_B' ? 1 : undefined;
  const winnerSideB = winnerSideA;

  return (
    <div
      className="match-share-card flex flex-col items-stretch w-full max-w-md mx-auto"
      style={{ minWidth: 340, maxWidth: 420, background: 'transparent', borderRadius: 0, overflow: 'visible' }}
    >
      {/* Header bleu compact avec round en badge blanc, coins arrondis */}
      <div
        className="flex flex-col items-center justify-center px-6 pt-5 pb-2"
        style={{ background: 'var(--color-primary)', color: 'var(--color-on-primary)', borderTopLeftRadius: 24, borderTopRightRadius: 24 }}
      >
        {displayName && (
          <div className="text-base font-bold truncate w-full text-center" style={{ letterSpacing: 0.2 }}>{displayName}</div>
        )}
        {game.round?.stage && (
          <div className="flex justify-center mt-2 mb-1">
            <span className="px-4 py-1 rounded-full text-sm font-semibold" style={{ background: '#fff', color: 'var(--color-primary)' }}>
              {formatStageLabel(game.round.stage)}
            </span>
          </div>
        )}
      </div>

      {/* Scoreboard compact, transparent background */}
      <div className="flex flex-col gap-2 px-5 pb-3">
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
          hideBackground={true}
        />
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
          hideBackground={true}
        />
      </div>

      {/* Footer sombre compact, coins arrondis */}
      <div
        className="w-full text-center py-2 text-xs font-semibold tracking-wider"
        style={{ background: 'var(--color-primary)', color: 'var(--color-on-primary)', borderBottomLeftRadius: 24, borderBottomRightRadius: 24 }}
      >
        {displayClub && <div className="text-xs text-white/70 mb-1">{displayClub}</div>}
        <div>www.padelrounds.com</div>
      </div>
    </div>
  );
}
