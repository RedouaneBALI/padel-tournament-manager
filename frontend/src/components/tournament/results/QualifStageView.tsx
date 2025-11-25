import KnockoutBracket from '@/src/components/round/KnockoutBracket';
import { calculateMatchPositions } from '@/src/utils/bracket';
import type { Tournament } from '@/src/types/tournament';

export default function QualifStageView({
  tournament,
  tournamentId,
}: {
  tournament: Tournament;
  tournamentId: string;
}) {
  const qualifRounds = (tournament.rounds ?? []).filter(
    (round) => ['Q1', 'Q2', 'Q3'].includes(round.stage)
  );
  const hasQualifs = qualifRounds.length > 0;

  const maxPosition = (() => {
    if (!hasQualifs) return 0;
    const matchPositions = calculateMatchPositions(qualifRounds);
    if (matchPositions.length === 0) return 0;
    return Math.max(...matchPositions.flat()) + 200;
  })();

  if (!hasQualifs)
    return <p className="text-muted-foreground">Les qualifications n'ont pas encore été générées.</p>;

  return (
    <div className="w-full">
      <div
        id="qualif-bracket-container"
        className="relative overflow-auto border border-border rounded-lg px-2 py-6 md:p-8 bg-background stage-min-height"
        style={maxPosition ? { ['--stage-min-height' as any]: `${maxPosition}px` } as React.CSSProperties : undefined}
      >
        <div className="w-max mx-0 md:mx-auto">
          <KnockoutBracket rounds={qualifRounds} tournamentId={tournamentId} />
        </div>
      </div>
    </div>
  );
}
