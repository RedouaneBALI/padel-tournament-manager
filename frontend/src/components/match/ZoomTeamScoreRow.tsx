import React from 'react';
import type { PlayerPair, TeamSide } from '@/src/types/game';
import type { GamePoint } from '@/src/types/score';
import { cn } from '@/src/lib/utils';
import TeamRow from '@/src/components/ui/TeamRow';
import { Plus, Loader2 } from 'lucide-react';
import { formatGamePoint } from '@/src/utils/zoomMatchUtils';
import { getTeamTheme } from '@/src/utils/teamTheme';

function SetScoresDisplay({ setScores }: { setScores: (number | null)[] }) {
  return (
    <div className="flex gap-[2px] sm:gap-2">
      {setScores.map((score, i) => (
        <div
          key={`set-${i}`}
          className={cn(
            'w-6 h-8 sm:w-9 sm:h-10 flex items-center justify-center rounded text-base sm:text-xl font-bold tabular-nums',
            score !== null ? 'text-foreground bg-muted/50' : 'opacity-0'
          )}
        >
          {score ?? '-'}
        </div>
      ))}
    </div>
  );
}

function CurrentPointDisplay({ isTieBreakActive, displayPoint, isTeamA }: {
  isTieBreakActive: boolean;
  displayPoint: string | number;
  isTeamA: boolean;
}) {
  const theme = getTeamTheme(isTeamA);
  return (
    <div
      className={cn(
        'w-9 h-9 sm:w-14 sm:h-12 flex items-center justify-center rounded-lg font-bold text-base sm:text-2xl tabular-nums shadow-inner transition-colors border',
        theme.point.bg,
        theme.point.text,
        theme.point.border
      )}
    >
      <span className={cn(isTieBreakActive ? 'text-sm sm:text-2xl' : '')}>
        {displayPoint}
      </span>
    </div>
  );
}

function ActionButton({
  isTeamA,
  loading,
  onClick,
  disabled,
  isFinished = false,
}: {
  isTeamA: boolean;
  loading: boolean;
  onClick: () => void;
  disabled: boolean;
  isFinished?: boolean;
}) {
  const theme = getTeamTheme(isTeamA);
  const isDisabled = disabled || isFinished;

  return (
    <button
      type="button"
      disabled={isDisabled}
      onClick={onClick}
      className={cn(
        'w-9 h-9 sm:w-12 sm:h-12 flex items-center justify-center rounded-full shadow-sm transition-all active:scale-95 border text-white',
        theme.button.bg,
        !isDisabled && cn(theme.button.hover, theme.button.shadow),
        theme.button.border,
        isDisabled && 'opacity-50 cursor-not-allowed',
        loading && 'opacity-70'
      )}
      aria-label={`Ajouter point à ${isTeamA ? 'Équipe A' : 'Équipe B'}`}
      title={isFinished ? 'Le match est terminé' : undefined}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 sm:w-5 sm:h-5 animate-spin" />
      ) : (
        <Plus className="w-5 h-5 sm:w-7 sm:h-7" strokeWidth={3} />
      )}
    </button>
  );
}

interface ZoomTeamScoreRowProps {
  team: PlayerPair | null;
  teamIndex: number;
  gamePoint: GamePoint | null | undefined;
  setScores: (number | null)[];
  tieBreakPoint: number | null | undefined;
  teamSide: TeamSide;
  editable: boolean;
  loading: boolean;
  onPointChange: (teamSide: TeamSide) => void;
  winnerSide?: number;
  isFinished?: boolean;
}

export default function ZoomTeamScoreRow({
  team,
  teamIndex,
  gamePoint,
  setScores,
  tieBreakPoint,
  teamSide,
  editable,
  loading,
  onPointChange,
  winnerSide,
  isFinished = false,
}: ZoomTeamScoreRowProps) {
  const isTeamA = teamSide === 'TEAM_A';
  const isWinner = winnerSide === teamIndex;

  const isTieBreakActive = tieBreakPoint !== null && tieBreakPoint !== undefined;
  const displayPoint = isTieBreakActive ? tieBreakPoint : formatGamePoint(gamePoint);
  const themeColor = isTeamA ? 'blue' : 'rose';

  return (
    <div
      className={cn(
        'relative flex items-center px-2 py-3 sm:p-4 rounded-xl transition-all duration-300',
        isWinner
          ? 'bg-gradient-to-r from-yellow-500/10 to-transparent border border-yellow-500/20'
          : 'bg-card border border-transparent shadow-sm'
      )}
    >
      {/* Team name */}
      <div className="flex-1 min-w-0 mr-1 sm:mr-4">
        <TeamRow
          team={team}
          teamIndex={teamIndex}
          winnerSide={winnerSide}
          fontSize="text-sm sm:text-lg"
          showChampion={false}
          themeColor={themeColor}
        />
      </div>

      {/* Scores section */}
      <div className="flex items-center gap-1.5 sm:gap-4">
        <SetScoresDisplay setScores={setScores} />

        {!isFinished && (
          <CurrentPointDisplay
            isTieBreakActive={isTieBreakActive}
            displayPoint={displayPoint}
            isTeamA={isTeamA}
          />
        )}

        {editable && (
          <ActionButton
            isTeamA={isTeamA}
            loading={loading}
            onClick={() => onPointChange(teamSide)}
            disabled={loading || typeof winnerSide !== 'undefined'}
            isFinished={isFinished}
          />
        )}
      </div>
    </div>
  );
}

