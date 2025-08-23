import GroupStageResults from '@/src/components/round/GroupStageResults';
import type { Tournament } from '@/src/types/tournament';

export default function GroupStageView({ tournament }: { tournament: Tournament }) {
  return (
    <div className="relative overflow-auto border border-border rounded-lg px-2 py-6 md:p-8 bg-background">
      <GroupStageResults
        rounds={tournament.rounds ?? []}
        nbQualifiedByPool={tournament.config.nbQualifiedByPool ?? 1}
      />
    </div>
  );
}

