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
      className="match-share-card"
      style={{
        background: 'var(--color-primary)',
        padding: 24,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100%',
      }}
    >
      <div
        style={{
          background: 'var(--color-primary)',
          borderRadius: 20,
          overflow: 'hidden',
          width: '100%',
          maxWidth: 420,
          boxShadow: '0 4px 24px rgba(0,0,0,0.15)',
          border: '2px solid rgba(255,255,255,0.1)',
        }}
      >
        {/* Header */}
        <div
          style={{
            background: 'linear-gradient(180deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.05) 100%)',
            padding: '16px 20px 12px',
            textAlign: 'center',
            borderBottom: '1px solid rgba(255,255,255,0.1)',
          }}
        >
          {displayName && (
            <div style={{ color: '#fff', fontSize: 18, fontWeight: 700, letterSpacing: 0.3, marginBottom: game.round?.stage ? 10 : 0, textShadow: '0 1px 2px rgba(0,0,0,0.2)' }}>
              {displayName}
            </div>
          )}
          {game.round?.stage && (
            <span style={{ display: 'inline-block', background: '#fff', color: 'var(--color-primary)', padding: '6px 16px', borderRadius: 20, fontSize: 13, fontWeight: 600, letterSpacing: 0.5, textTransform: 'uppercase' }}>
              {formatStageLabel(game.round.stage)}
            </span>
          )}
        </div>

        {/* Scoreboard */}
        <div style={{ background: 'rgba(255,255,255,0.08)', padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
          <ZoomTeamScoreRow team={teams[0]} teamIndex={0} gamePoint={game.score?.currentGamePointA} setScores={setScoresA} tieBreakPoint={tieBreakPointA} teamSide="TEAM_A" editable={false} loading={false} onPointChange={() => {}} winnerSide={winnerSideA} isFinished={game.finished} hideBackground={true} shareMode={true} />
          <div style={{ height: 1, background: 'rgba(255,255,255,0.1)', margin: '4px 0' }} />
          <ZoomTeamScoreRow team={teams[1]} teamIndex={1} gamePoint={game.score?.currentGamePointB} setScores={setScoresB} tieBreakPoint={tieBreakPointB} teamSide="TEAM_B" editable={false} loading={false} onPointChange={() => {}} winnerSide={winnerSideB} isFinished={game.finished} hideBackground={true} shareMode={true} />
        </div>

        {/* Footer */}
        <div style={{ background: 'rgba(0,0,0,0.15)', padding: '12px 16px', textAlign: 'center', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
          {displayClub && <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12, marginBottom: 4, fontWeight: 500 }}>{displayClub}</div>}
          <div className="text-gold" style={{ fontSize: 13, fontWeight: 600, letterSpacing: 0.5 }}>www.padelrounds.com</div>
        </div>
      </div>
    </div>
  );
}
