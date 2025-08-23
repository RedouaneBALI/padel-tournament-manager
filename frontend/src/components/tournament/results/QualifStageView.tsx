import KnockoutBracket from '@/src/components/round/KnockoutBracket';
import type { Tournament } from '@/src/types/tournament';

export default function QualifStageView({
  tournament,
  tournamentId,
}: {
  tournament: Tournament;
  tournamentId: string;
}) {
  const qualifRounds = (tournament.rounds ?? []).filter((round) => round.stage === 'Q1');
  const hasQualifs = qualifRounds.length > 0;
  console.log(tournament.rounds);

  return (
    <div className="relative overflow-auto border border-border rounded-lg px-2 py-6 md:p-8 bg-background">
      {hasQualifs ? (
        <KnockoutBracket rounds={qualifRounds} tournamentId={tournamentId} />
      ) : (
        <p className="text-muted-foreground">Les qualifications n'ont pas encore été générées.</p>
      )}
    </div>
  );
}

