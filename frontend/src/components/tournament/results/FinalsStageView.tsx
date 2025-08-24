import KnockoutBracket from '@/src/components/round/KnockoutBracket';
import BracketHeader from '@/src/components/tournament/BracketHeader';
import { calculateMatchPositions } from '@/src/utils/bracket';
import { exportBracketAsImage } from '@/src/utils/imageExport';
import type { Tournament } from '@/src/types/tournament';

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
  const finalsRounds = (() => {
    const r = tournament.rounds ?? [];
    if (isGroupStageFormat) return r.filter((round) => round.stage !== 'GROUPS');
    if (isQualifStageFormat) return r.filter((round) => round.stage !== 'Q1' && round.stage !== 'Q2' && round.stage !== 'Q3');
    return r;
  })();

  const hasFinals = finalsRounds.length > 0;
  const maxPosition = (() => {
    if (!hasFinals) return 0;
    const matchPositions = calculateMatchPositions(finalsRounds);
    if (matchPositions.length === 0) return 0;
    return Math.max(...matchPositions.flat()) + 200;
  })();

  if (!hasFinals)
    return <p className="text-muted-foreground">La phase finale n'a pas encore été générée.</p>;

  return (
    <div className="w-full">
      <BracketHeader onExport={() => exportBracketAsImage('bracket-container')} />
      <div
        id="bracket-container"
        className="relative overflow-auto border border-border rounded-lg px-2 py-6 md:p-8 bg-background"
        style={{ minHeight: maxPosition ? `${maxPosition}px` : undefined }}
      >
        <div className="w-max mx-0 md:mx-auto">
          <KnockoutBracket rounds={finalsRounds} tournamentId={tournamentId} />
        </div>
      </div>
    </div>
  );
}

