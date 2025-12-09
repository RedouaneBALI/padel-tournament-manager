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
import { useExport } from '@/src/contexts/ExportContext';
import { exportBracketAsImage } from '@/src/utils/imageExport';

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
  const { setExportFunction } = useExport();

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

  const isGroupStageFormat = tournament?.config?.format === 'GROUPS_KO';
  const isQualifStageFormat = tournament?.config?.format === 'QUALIF_KO';

  const hasMatchesInFinalPhase = useMemo(() => {
    if (!tournament?.rounds) return false;
    const finalPhaseStages = [Stage.R64, Stage.R32, Stage.R16, Stage.QUARTERS, Stage.SEMIS, Stage.FINAL, Stage.WINNER];
    return tournament.rounds.some(round =>
      finalPhaseStages.includes(round.stage) &&
      round.games?.some(game => game.score !== null)
    );
  }, [tournament]);

  const currentStage = tournament?.currentRoundStage;
  const defaultView =
    isGroupStageFormat && currentStage !== Stage.GROUPS
      ? VIEW_PHASE_FINALE
      : isGroupStageFormat
      ? VIEW_CLASSEMENT
      : isQualifStageFormat
      ? (hasMatchesInFinalPhase ? VIEW_PHASE_FINALE : VIEW_QUALIF)
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

  useEffect(() => {
    if (!isLoading && tournament && !queryView) {
      setView(defaultView);
    }
  }, [isLoading, tournament, queryView, defaultView, setView]);

  // Register export function based on active view
  useEffect(() => {
    if (isLoading || !tournament) {
      setExportFunction(null);
      return;
    }

    if (activeView === VIEW_CLASSEMENT) {
      // No export for group stage view (tables)
      setExportFunction(null);
    } else if (activeView === VIEW_QUALIF) {
      setExportFunction(() => exportBracketAsImage('qualif-bracket-container', 'qualifications.png'));
    } else if (activeView === VIEW_PHASE_FINALE) {
      setExportFunction(() => exportBracketAsImage('finals-bracket-container', 'phase_finale.png'));
    }

    return () => setExportFunction(null);
  }, [activeView, isLoading, tournament, setExportFunction]);

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
              {isGroupStageFormat ? 'Poules' : isQualifStageFormat ? 'Qualif' : 'Poules'}
            </button>
            <button
              onClick={() => setView(VIEW_PHASE_FINALE)}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeView === VIEW_PHASE_FINALE
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              {isGroupStageFormat || isQualifStageFormat ? 'Tableau principal' : 'Phase finale'}
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