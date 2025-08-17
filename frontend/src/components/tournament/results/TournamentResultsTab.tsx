"use client";
import { useEffect, useMemo, useState } from 'react';
import { ArrowDownTrayIcon } from '@heroicons/react/24/solid';
import { fetchTournament } from '@/src/api/tournamentApi';
import type { Round } from '@/src/types/round';
import type { Tournament } from '@/src/types/tournament';
import { toPng } from 'html-to-image';
import KnockoutBracket from '@/src/components/round/KnockoutBracket';
import GroupStageResults from '@/src/components/round/GroupStageResults';
import { Stage } from '@/src/types/stage';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import BracketHeader from '@/src/components/tournament/BracketHeader';
import { calculateMatchPositions } from '@/src/utils/bracket';
import { exportBracketAsImage } from '@/src/utils/imageExport';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import { VIEW_CLASSEMENT, VIEW_PHASE_FINALE } from '@/src/constants/views';

interface TournamentResultsTabProps {
  tournamentId: string;
}

export default function TournamentResultsTab({ tournamentId}: TournamentResultsTabProps) {
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
        console.error("Erreur lors du chargement des rounds : " + err);
      } finally {
        setIsLoading(false);
      }
    }

    load();
  }, [tournamentId]);

  const isGroupStageFormat = tournament?.tournamentFormat === "GROUP_STAGE";
  const currentStage = tournament?.currentRoundStage;
  const defaultView =
    isGroupStageFormat && currentStage !== Stage.GROUPS
      ? VIEW_PHASE_FINALE
      : (isGroupStageFormat ? VIEW_CLASSEMENT : VIEW_PHASE_FINALE);
  const queryView = searchParams?.get('view');
  const activeView = (queryView === VIEW_CLASSEMENT || queryView === VIEW_PHASE_FINALE) ? queryView : defaultView;

  const finalsRounds = useMemo(() => {
    const r = tournament?.rounds ?? [];
    return r.filter((round) => round.stage !== Stage.GROUPS);
  }, [tournament]);

  const hasFinals = useMemo(() => finalsRounds.length > 0, [finalsRounds]);

  const maxPosition = useMemo(() => {
    if (!hasFinals) return 0;
    const matchPositions = calculateMatchPositions(finalsRounds);
    if (matchPositions.length === 0) return 0;
    return Math.max(...matchPositions.flat()) + 200;
  }, [hasFinals, finalsRounds]);

  const setView = (view: string) => {
    const params = new URLSearchParams(searchParams?.toString());
    params.set('view', view);
    router.replace(`${pathname}?${params.toString()}`);
  };

  if (isLoading) {
    return <CenteredLoader />;
  }
  if (!tournament) {
    return <CenteredLoader />;
  }
  if (!isLoading && (tournament?.rounds ?? []).length === 0) {
    return <p className="text-muted-foreground">Aucun tirage généré pour le moment.</p>;
  }

  return (
    <div className="w-full">
      {/* Sub-tabs only if group stage format */}
      {isGroupStageFormat && (
        <div className="mb-4 border-b border-border">
          <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets tableau">
            <button
              onClick={() => setView(VIEW_CLASSEMENT)}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeView === VIEW_CLASSEMENT
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              Poules
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

      {/* Poules view only for group stage format */}
      {isGroupStageFormat && activeView === VIEW_CLASSEMENT && (
        <div className="relative overflow-auto border border-border rounded-lg px-2 py-6 md:p-8 bg-background">
          <GroupStageResults
            rounds={tournament?.rounds ?? []}
            nbQualifiedByPool={tournament?.nbQualifiedByPool ?? 1}
          />
        </div>
      )}

      {/* Phase finale view */}
      {isGroupStageFormat ? (
        activeView === VIEW_PHASE_FINALE && (
          hasFinals ? (
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
          ) : (
            <p className="text-muted-foreground">La phase finale n'a pas encore été générée.</p>
          )
        )
      ) : (
        // If not group stage format: show knockout bracket directly, no tabs
        hasFinals ? (
          <div className="w-full">
            <BracketHeader onExport={() => exportBracketAsImage('bracket-container')} />

            <div
              id="bracket-container"
              className="relative overflow-auto border border-border rounded-lg px-2 md:p-8 bg-background"
              style={{ minHeight: maxPosition ? `${maxPosition}px` : undefined }}
            >
              <KnockoutBracket rounds={finalsRounds} tournamentId={tournamentId} />
            </div>
          </div>
        ) : (
          <p className="text-muted-foreground">La phase finale n'a pas encore été générée.</p>
        )
      )}
    </div>
  );
}