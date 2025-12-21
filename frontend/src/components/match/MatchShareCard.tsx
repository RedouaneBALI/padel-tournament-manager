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
  const { setScoresA, setScoresB, tieBreakPointA, tieBreakPointB } = processDisplayData(game, matchFormat ?? null);
  const winnerSideA = game.winnerSide === 'TEAM_A' ? 0 : game.winnerSide === 'TEAM_B' ? 1 : undefined;
  const winnerSideB = winnerSideA;

  return (
    <div
      className="match-share-card flex flex-col items-stretch w-full max-w-md mx-auto"
      style={{
        background: 'linear-gradient(180deg, #243a6e 0%, #1b2d5e 30%, #1b2d5e 70%, #152347 100%)',
        borderRadius: 0,
        overflow: 'hidden',
        margin: 0,
        padding: '32px 24px'
      }}
    >
      {/* Header - plus de bordure, juste du contenu */}
      <div
        className="flex flex-col items-center justify-center px-4"
        style={{ color: 'var(--color-on-primary)', paddingTop: '12px', paddingBottom: '16px' }}
      >
        {displayName && (
          <div className="text-lg font-bold truncate w-full text-center" style={{ letterSpacing: 0.3 }}>{displayName}</div>
        )}
        {game.round?.stage && (
          <div className="flex justify-center" style={{ marginTop: '12px' }}>
            <span className="px-5 py-1.5 rounded-full text-sm font-semibold" style={{ background: '#fff', color: 'var(--color-primary)' }}>
              {formatStageLabel(game.round.stage)}
            </span>
          </div>
        )}
      </div>

      {/* Séparateur subtil */}
      <div style={{ height: 1, background: 'rgba(255, 255, 255, 0.15)', margin: '0 12px' }} />

      {/* Scoreboard - fond très léger, sans bordure */}
      <div className="flex flex-col" style={{ background: 'rgba(255, 255, 255, 0.06)', padding: '16px', margin: '0', gap: '0px', borderRadius: '12px', marginTop: '12px' }}>
        <ZoomTeamScoreRow team={teams[0]} teamIndex={0} gamePoint={game.score?.currentGamePointA} setScores={setScoresA} tieBreakPoint={tieBreakPointA} teamSide="TEAM_A" editable={false} loading={false} onPointChange={() => {}} winnerSide={winnerSideA} isFinished={game.finished} hideBackground={true} shareMode={true} />
        <div style={{ height: 1, background: 'rgba(255, 255, 255, 0.1)', margin: '8px 0' }} />
        <ZoomTeamScoreRow team={teams[1]} teamIndex={1} gamePoint={game.score?.currentGamePointB} setScores={setScoresB} tieBreakPoint={tieBreakPointB} teamSide="TEAM_B" editable={false} loading={false} onPointChange={() => {}} winnerSide={winnerSideB} isFinished={game.finished} hideBackground={true} shareMode={true} />
      </div>

      {/* Footer - intégré naturellement */}
      <div
        className="w-full text-center text-xs font-medium tracking-wider"
        style={{ color: 'rgba(255, 255, 255, 0.6)', padding: '16px 0 8px', lineHeight: '1.4' }}
      >
        {displayClub && <div style={{ marginBottom: '4px' }}>{displayClub}</div>}
        <div style={{ color: 'rgba(255, 255, 255, 0.8)' }}>www.padelrounds.com</div>
      </div>
    </div>
  );
}
