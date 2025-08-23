"use client";
import { useEffect, useState, useMemo } from 'react';
import { fetchTournament } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import { Stage } from '@/src/types/stage';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import { VIEW_CLASSEMENT, VIEW_PHASE_FINALE } from '@/src/constants/views';
import GroupStageView from './GroupStageView';
import QualifStageView from './QualifStageView';
import FinalsStageView from './FinalsStageView';

const VIEW_QUALIF = 'qualif';

interface TournamentResultsTabProps {
  tournamentId: string;
}

export default function TournamentResultsTab({ tournamentId }: TournamentResultsTabProps) {
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    async function load() {
      setIsLoading(true);
      try {
        const data = await fetchTournament(tournamentId);
        setTournament(data);
      } catch (err) {
        console.error("Erreur lors du chargement du tournoi : " + err);
      } finally {
        setIsLoading(false);
      }
    }
    load();
  }, [tournamentId]);

  const isGroupStageFormat = tournament?.format === 'GROUPS_KO';
  const isQualifStageFormat = tournament?.format === 'QUALIF_KO';

  const currentStage = tournament?.currentRoundStage;
  const defaultView =
    isGroupStageFormat && currentStage !== Stage.GROUPS
      ? VIEW_PHASE_FINALE
      : isGroupStageFormat
      ? VIEW_CLASSEMENT
      : isQualifStageFormat
      ? VIEW_QUALIF
      : VIEW_PHASE_FINALE;

  const queryView = searchParams?.get('view');
  const activeView =
    (queryView === VIEW_CLASSEMENT || queryView === VIEW_PHASE_FINALE || queryView === VIEW_QUALIF)
      ? queryView
      : defaultView;

  const setView = (view: string) => {
    const params = new URLSearchParams(searchParams?.toString());
    params.set('view', view);
    router.replace(`${pathname}?${params.toString()}`);
  };

  if (isLoading) return <CenteredLoader />;
  if (!tournament) return <CenteredLoader />;
  if (!isLoading && (tournament?.rounds ?? []).length === 0)
    return <p className="text-muted-foreground">Aucun tirage généré pour le moment.</p>;

  // Onglets pour GROUPS_KO et QUALIF_KO
  const showTabs = isGroupStageFormat || isQualifStageFormat;

  return (
    <div className="w-full">
      {showTabs && (
        <div className="mb-4 border-b border-border">
          <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets tableau">
            <button
              onClick={() => setView(isQualifStageFormat ? VIEW_QUALIF : VIEW_CLASSEMENT)}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeView === (isQualifStageFormat ? VIEW_QUALIF : VIEW_CLASSEMENT)
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              {isGroupStageFormat ? 'Poules' : 'Qualifications'}
            </button>
            <button
              onClick={() => setView(VIEW_PHASE_FINALE)}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeView === VIEW_PHASE_FINALE
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              Phase finale
            </button>
          </nav>
        </div>
      )}

      {/* Vue Poules */}
      {isGroupStageFormat && activeView === VIEW_CLASSEMENT && (
        <GroupStageView tournament={tournament} />
      )}

      {/* Vue Qualifications */}
      {isQualifStageFormat && activeView === VIEW_QUALIF && (
        <QualifStageView tournament={tournament} tournamentId={tournamentId} />
      )}

      {/* Vue Phase finale */}
      {(isGroupStageFormat || isQualifStageFormat) && activeView === VIEW_PHASE_FINALE && (
        <FinalsStageView
          tournament={tournament}
          tournamentId={tournamentId}
          isGroupStageFormat={isGroupStageFormat}
          isQualifStageFormat={isQualifStageFormat}
        />
      )}

      {/* Vue classique */}
      {!isGroupStageFormat && !isQualifStageFormat && (
        <FinalsStageView
          tournament={tournament}
          tournamentId={tournamentId}
          isGroupStageFormat={false}
          isQualifStageFormat={false}
        />
      )}
    </div>
  );
}