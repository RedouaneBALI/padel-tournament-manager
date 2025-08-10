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

const VIEW_CLASSEMENT = 'classement';
const VIEW_PHASE_FINALE = 'phase-finale';

// Fonction pour calculer la position verticale correcte de chaque match
function calculateMatchPositions(rounds: Round[]) {
  if (rounds.length === 0) return [];

  const positions: number[][] = [];

  // Calculer les positions pour chaque round
  rounds.forEach((round, roundIndex) => {
    const roundPositions: number[] = [];
    const nbMatches = round.games.length;

    if (roundIndex === 0) {
      // Premier round : positions équidistantes
      const baseSpacing = 160; // Espacement de base entre les matchs
      for (let i = 0; i < nbMatches; i++) {
        roundPositions.push(i * baseSpacing);
      }
    } else {
      // Rounds suivants : chaque match se positionne entre deux matchs précédents
      const previousPositions = positions[roundIndex - 1];
      for (let i = 0; i < nbMatches; i++) {
        // Position au milieu des deux matchs précédents
        const pos1 = previousPositions[i * 2] || 0;
        const pos2 = previousPositions[i * 2 + 1] || 0;
        roundPositions.push((pos1 + pos2) / 2);
      }
    }

    positions.push(roundPositions);
  });

  return positions;
}

// Composant principal
interface TournamentResultsTabProps {
  tournamentId: string;
}

export default function TournamentResultsTab({ tournamentId}: TournamentResultsTabProps) {
  const [tournament, setTournament] = useState<Tournament | null>(null);

  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    async function load() {
      try {
        const data = await fetchTournament(tournamentId);
        setTournament(data);
      } catch (err) {
        console.error("Erreur lors du chargement des rounds : " + err);
      }
    }

    load();
  }, [tournamentId]);

  const firstRoundStage = tournament?.rounds?.[0]?.stage;
  const defaultView = firstRoundStage === Stage.GROUPS ? VIEW_CLASSEMENT : VIEW_PHASE_FINALE;
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

const exportBracketAsImage = async () => {
  const node = document.getElementById('bracket-container');
  if (!node) return;

  const originalStyle = {
    overflow: node.style.overflow,
    width: node.style.width,
    height: node.style.height,
    maxWidth: node.style.maxWidth,
  };

  try {
    // Forcer la taille exacte du contenu scrollable
    const scrollWidth = node.scrollWidth;
    const scrollHeight = node.scrollHeight;

    node.style.overflow = 'visible';
    node.style.width = `${scrollWidth}px`;
    node.style.height = `${scrollHeight}px`;
    node.style.maxWidth = 'none';

    // Attendre que le style soit bien appliqué
    await new Promise((resolve) => setTimeout(resolve, 50));

    // Générer l'image PNG avec la taille complète
    const dataUrl = await toPng(node, {
      width: scrollWidth,
      height: scrollHeight,
    });

    const link = document.createElement('a');
    link.download = 'bracket.png';
    link.href = dataUrl;
    link.click();
  } catch (err) {
    console.error('Erreur lors de l’export en image :', err);
  } finally {
    // Restaurer les styles d'origine
    node.style.overflow = originalStyle.overflow;
    node.style.width = originalStyle.width;
    node.style.height = originalStyle.height;
    node.style.maxWidth = originalStyle.maxWidth;
  }
};

  const setView = (view: string) => {
    const params = new URLSearchParams(searchParams?.toString());
    params.set('view', view);
    router.replace(`${pathname}?${params.toString()}`);
  };

  if ((tournament?.rounds ?? []).length === 0) {
    return <p className="text-gray-500">Aucun tirage généré pour le moment.</p>;
  }

  return (
    <div className="w-full">
      {/* Sub-tabs */}
      <div className="mb-4 border-b border-gray-200">
        <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets tableau">
          <button
            onClick={() => setView(VIEW_CLASSEMENT)}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
              activeView === VIEW_CLASSEMENT
                ? 'border-[#1b2d5e] text-primary'
                : 'border-transparent text-gray-500 hover:text-primary'
            }`}
          >
            Poules
          </button>
          <button
            onClick={() => setView(VIEW_PHASE_FINALE)}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
              activeView === VIEW_PHASE_FINALE
                ? 'border-[#1b2d5e] text-primary'
                : 'border-transparent text-gray-500 hover:text-primary'
            }`}
          >
            Phase finale
          </button>
        </nav>
      </div>

      {activeView === VIEW_CLASSEMENT && (
        <GroupStageResults rounds={tournament?.rounds ?? []} nbQualifiedByPool={tournament?.nbQualifiedByPool ?? 1} />
      )}

      {activeView === VIEW_PHASE_FINALE && (
        hasFinals ? (
          <div className="w-full">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Arbre du tournoi</h2>
              <button
                onClick={exportBracketAsImage}
                className="p-2 text-white bg-gray-500 hover:bg-blue-700 rounded-md"
                title="Exporter en PNG"
              >
                <ArrowDownTrayIcon className="h-5 w-5" />
              </button>
            </div>

            <div
              id="bracket-container"
              className="relative overflow-auto border border-gray-200 rounded-lg p-8 bg-gray-50"
              style={{ minHeight: maxPosition ? `${maxPosition}px` : undefined }}
            >
              <KnockoutBracket rounds={finalsRounds} tournamentId={tournamentId} />
            </div>
          </div>
        ) : (
          <p className="text-gray-500">La phase finale n'a pas encore été générée.</p>
        )
      )}
    </div>
  );
}