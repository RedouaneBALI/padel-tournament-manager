import React from 'react';
import type { PlayerPair, TeamSide } from '@/src/types/game';
import type { GamePoint } from '@/src/types/score';
import { cn } from '@/src/lib/utils';
import TeamRow from '@/src/components/ui/TeamRow';
import { Plus, Loader2 } from 'lucide-react';
import { formatGamePoint } from '@/src/utils/zoomMatchUtils';
import { getTeamTheme } from '@/src/utils/teamTheme';

function SetScoresDisplay({ setScores, shareMode = false, isWinner = false }: { setScores: (number | null)[]; shareMode?: boolean; isWinner?: boolean }) {
  const nonNullScores = setScores.filter(score => score !== null);
  const justifyEnd = nonNullScores.length < 3;

  return (
    <div className={cn("flex gap-[2px] sm:gap-2", justifyEnd && "justify-end")}>
      {setScores.map((score, i) => (
        score !== null && (
          <div
            key={`set-${i}`}
            className={cn(
              'w-6 h-8 sm:w-9 sm:h-10 flex items-center justify-center rounded text-base sm:text-xl font-bold tabular-nums',
              shareMode
                ? (isWinner ? 'text-gold bg-white/15' : 'text-white/80 bg-white/15')
                : 'text-foreground bg-muted/50'
            )}
          >
            {score}
          </div>
        )
      ))}
    </div>
  );
}

function CurrentPointDisplay({ isTieBreakActive, displayPoint, isTeamA, shareMode = false, isWinner = false }: {
  isTieBreakActive: boolean;
  displayPoint: string | number;
  isTeamA: boolean;
  shareMode?: boolean;
  isWinner?: boolean;
}) {
  const theme = getTeamTheme(isTeamA);

  if (shareMode) {
    // Pour le mode partage, utiliser des couleurs adaptées au fond bleu
    const bgColor = isTeamA ? 'bg-blue-400/30' : 'bg-rose-400/30';
    const textColor = isWinner ? 'text-gold' : (isTeamA ? 'text-blue-200' : 'text-rose-200');
    const borderColor = isTeamA ? 'border-blue-400/50' : 'border-rose-400/50';

    return (
      <div
        className={cn(
          'w-9 h-9 sm:w-14 sm:h-12 flex items-center justify-center rounded-lg font-bold text-base sm:text-2xl tabular-nums shadow-inner transition-colors border',
          bgColor,
          textColor,
          borderColor
        )}
      >
        <span className={cn(isTieBreakActive ? 'text-sm sm:text-2xl' : '')}>
          {displayPoint}
        </span>
      </div>
    );
  }

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

  const handleClick = () => {
    console.log('ActionButton clicked for team:', isTeamA ? 'TEAM_A' : 'TEAM_B', 'disabled:', isDisabled);
    if (!isDisabled) {
      onClick();
    }
  };

  return (
    <button
      type="button"
      disabled={isDisabled}
      onClick={handleClick}
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
  winnerSide?: TeamSide;
  isFinished?: boolean;
  hideBackground?: boolean;
  shareMode?: boolean;
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
  hideBackground = false,
  shareMode = false,
}: ZoomTeamScoreRowProps) {
  const isTeamA = teamSide === 'TEAM_A';
  const isWinner = winnerSide === (isTeamA ? 'TEAM_A' : 'TEAM_B');

  const isTieBreakActive = tieBreakPoint !== null && tieBreakPoint !== undefined;
  const displayPoint = isTieBreakActive ? tieBreakPoint : formatGamePoint(gamePoint);
  const themeColor = isTeamA ? 'blue' : 'rose';

  return (
    <div
      className={cn(
        'relative flex items-center rounded-xl transition-all duration-300',
        shareMode ? 'px-1 py-0' : 'px-2 py-3 sm:p-4',
        isWinner && !shareMode ? 'winner-highlight' : hideBackground ? 'bg-transparent border-transparent shadow-none' : 'bg-card border border-transparent shadow-sm'
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
          textColor={shareMode && isWinner ? "gold" : shareMode ? "white" : undefined}
        />
      </div>

      {/* Scores section */}
      <div className={cn("flex items-center", shareMode ? "gap-1" : "gap-1.5 sm:gap-4")}>
        <SetScoresDisplay setScores={setScores} shareMode={shareMode} isWinner={isWinner} />

        {!isFinished && (
          <CurrentPointDisplay
            isTieBreakActive={isTieBreakActive}
            displayPoint={displayPoint}
            isTeamA={isTeamA}
            shareMode={shareMode}
            isWinner={isWinner}
          />
        )}

        {editable && (
          <ActionButton
            isTeamA={isTeamA}
            loading={loading}
            onClick={() => onPointChange(teamSide)}
            disabled={loading || !!winnerSide}
            isFinished={isFinished}
          />
        )}
      </div>
    </div>
  );
}
