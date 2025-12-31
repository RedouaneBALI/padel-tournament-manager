import { useState, useRef } from 'react';
import KnockoutBracket from '@/src/components/round/KnockoutBracket';
import { calculateMatchPositions } from '@/src/utils/bracket';
import type { Tournament } from '@/src/types/tournament';
import HideByeSwitch from '@/src/components/ui/HideByeSwitch';
import { useBracketZoom } from '@/src/hooks/useBracketZoom';

export default function FinalsStageView({
  tournament,
  tournamentId,
  isGroupStageFormat,
  isQualifStageFormat,
}: {
  tournament: Tournament;
  tournamentId: string;
  isGroupStageFormat: boolean;
  isQualifStageFormat: boolean;
}) {
  const [hideBye, setHideBye] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const scale = useBracketZoom(containerRef);

  const finalsRounds = (() => {
    const r = tournament.rounds ?? [];
    if (isGroupStageFormat) return r.filter((round) => round.stage !== 'GROUPS');
    if (isQualifStageFormat) return r.filter((round) => round.stage !== 'Q1' && round.stage !== 'Q2' && round.stage !== 'Q3');
    return r;
  })();

  const hasFinals = finalsRounds.length > 0;
  const hasBye = finalsRounds.some(round => round.games.some(game => game.teamA?.type === 'BYE' || game.teamB?.type === 'BYE'));

  // Modifié pour prendre en compte hideBye
  const maxPosition = (() => {
    if (!hasFinals) return 0;
    const matchPositions = calculateMatchPositions(finalsRounds, hideBye);
    if (matchPositions.length === 0) return 0;
    return Math.max(...matchPositions.flat()) + 150;
  })();

  // Adjust container height based on scale to prevent unnecessary scrolling
  const adjustedHeight = maxPosition ? maxPosition * scale : 0;


  if (!hasFinals)
    return <p className="text-muted-foreground">La phase finale n'a pas encore été générée.</p>;

  return (
    <div className="w-full">
      <HideByeSwitch hasBye={hasBye} hideBye={hideBye} setHideBye={setHideBye} />
      <div
        ref={containerRef}
        id="finals-bracket-container"
        className="relative overflow-auto bg-background pb-4 md:pb-8"
      >
        <div
          style={{
            height: adjustedHeight ? `${adjustedHeight}px` : undefined,
            width: 'fit-content',
            margin: '0 auto'
          }}
        >
          <div
            style={{
              transform: `scale(${scale})`,
              transformOrigin: 'top left',
              transition: 'transform 0.1s ease-out',
              width: 'max-content'
            }}
          >
            <KnockoutBracket rounds={finalsRounds} tournamentId={tournamentId} isQualif={false} hideBye={hideBye} />
          </div>
        </div>
      </div>
    </div>
  );
}