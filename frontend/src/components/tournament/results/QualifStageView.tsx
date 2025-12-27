import { useState } from 'react';
import KnockoutBracket from '@/src/components/round/KnockoutBracket';
import { calculateMatchPositions } from '@/src/utils/bracket';
import type { Tournament } from '@/src/types/tournament';
import HideByeSwitch from '@/src/components/ui/HideByeSwitch';

export default function QualifStageView({
  tournament,
  tournamentId,
}: {
  tournament: Tournament;
  tournamentId: string;
}) {
  const [hideBye, setHideBye] = useState(false);

  const qualifRounds = (tournament.rounds ?? []).filter(
    (round) => ['Q1', 'Q2', 'Q3'].includes(round.stage)
  );
  const hasQualifs = qualifRounds.length > 0;
  const hasBye = qualifRounds.some((round) => round.games.some((game) => game.teamA?.type === 'BYE' || game.teamB?.type === 'BYE'));

  const maxPosition = (() => {
    if (!hasQualifs) return 0;
    const matchPositions = calculateMatchPositions(qualifRounds, hideBye);
    if (matchPositions.length === 0) return 0;
    return Math.max(...matchPositions.flat()) + 150;
  })();

  if (!hasQualifs)
    return <p className="text-muted-foreground">Les qualifications n'ont pas encore été générées.</p>;

  return (
    <div className="w-full">
      <HideByeSwitch hasBye={hasBye} hideBye={hideBye} setHideBye={setHideBye} />
      <div
        id="qualif-bracket-container"
        className="relative overflow-auto bg-background stage-min-height pb-4 md:pb-8"
        style={
          maxPosition
            ? ({ ['--stage-min-height' as any]: `${maxPosition}px` } as React.CSSProperties)
            : undefined
        }
      >
        <div className="w-max mx-0 md:mx-auto">
          <KnockoutBracket rounds={qualifRounds} tournamentId={tournamentId} isQualif={true} hideBye={hideBye} />
        </div>
      </div>
    </div>
  );
}
