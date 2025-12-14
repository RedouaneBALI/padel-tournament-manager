'use client';

import React, { useEffect, useState, useRef, useMemo } from 'react';
import { ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/src/components/tournament/players/PlayerPairsTextarea';
import PlayerPairsList from '@/src/components/tournament/players/PlayerPairsList';
import { useRouter } from 'next/navigation';
import { confirmAlert } from 'react-confirm-alert';
import { generateDraw, savePlayerPairs } from '@/src/api/tournamentApi';
import type { InitializeDrawRequest } from '@/src/types/api/InitializeDrawRequest';
import { FileText } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { fetchTournament, fetchPairs } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import AdminTournamentPlayerAssignment from '@/src/components/admin/AdminTournamentPlayerAssignment';
import AdminTournamentSetupTab2Teams from '@/src/components/admin/AdminTournamentSetupTab2Teams';
import { getStageFromSize } from '@/src/types/stage';
import { hasTournamentStarted } from '@/src/utils/tournamentUtils';

interface Props {
  tournamentId: string;
}

function toTeamSlot(p: PlayerPair | null) {
  if (!p || !p.id) return { type: 'BYE' as const };
  return { type: 'NORMAL' as const, pairId: p.id };
}

function buildGames(slots: (PlayerPair | null)[]) {
  const games = [];
  for (let i = 0; i < slots.length; i += 2) {
    const a = slots[i];
    const b = slots[i + 1];
    games.push({
      teamA: toTeamSlot(a),
      teamB: toTeamSlot(b),
    });
  }
  return games;
}

function computeDefaultView(tournament: Tournament | null, pairs: PlayerPair[]): 'players' | 'assignment' {
  if (!tournament || !pairs.length) return 'players';

  const hasQualif = (tournament as any)?.config?.qualifSize > 0;
  const hasMain = (tournament as any)?.config?.mainDrawSize > 0;

  if (hasQualif && hasMain) {
    // Both qualif and main: check if qualif is filled
    const qualifSize = (tournament as any)?.config?.qualifSize || 0;
    const qualifFilled = pairs.length >= qualifSize;
    return qualifFilled ? 'assignment' : 'players';
  } else if (hasMain) {
    // Only main draw
    return 'assignment';
  } else {
    // Only qualif or none
    return 'players';
  }
}

function useTournamentData(tournamentId: string) {
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
  const [loadingPairs, setLoadingPairs] = useState(true);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [tournamentStarted, setTournamentStarted] = useState(false);
  const [loadingTournament, setLoadingTournament] = useState(true);

  useEffect(() => {
    async function loadTournament() {
      try {
        setLoadingTournament(true);
        const t = await fetchTournament(tournamentId);
        setTournament(t);
        setTournamentStarted((t as any)?.started || false);
      } catch (error) {
        console.error('Erreur lors du chargement du tournoi :', error);
      } finally {
        setLoadingTournament(false);
      }
    }
    loadTournament();
  }, [tournamentId]);

  useEffect(() => {
    async function loadPairs() {
      try {
        setLoadingPairs(true);
        const p = await fetchPairs(tournamentId, false, false);
        setPairs(p);
      } catch (error) {
        console.error('Erreur lors du chargement des paires :', error);
      } finally {
        setLoadingPairs(false);
      }
    }
    loadPairs();
  }, [tournamentId]);

  const defaultView = useMemo(() => computeDefaultView(tournament, pairs), [tournament, pairs]);

  return {
    pairs,
    setPairs,
    loadingPairs,
    tournament,
    setTournament,
    tournamentStarted,
    setTournamentStarted,
    loadingTournament,
    defaultView,
  };
}

export default function AdminTournamentSetupTab({ tournamentId }: Props) {
  const {
    pairs,
    setPairs,
    loadingPairs,
    tournament,
    setTournament,
    tournamentStarted,
    setTournamentStarted,
    loadingTournament,
    defaultView,
  } = useTournamentData(tournamentId);

  const [isGenerating, setIsGenerating] = useState(false);
  const router = useRouter();

  const [activeTab, setActiveTab] = useState<'players' | 'assignment'>(defaultView);

  useEffect(() => {
    setActiveTab(defaultView);
  }, [defaultView]);

  const [assignedSlots, setAssignedSlots] = useState<Array<PlayerPair | null>>([]);
  const [qualifSlots, setQualifSlots] = useState<Array<PlayerPair | null>>([]);
  const [mainSlots, setMainSlots] = useState<Array<PlayerPair | null>>([]);
  const assignedSlotsRef = useRef<Array<PlayerPair | null>>([]);
  const qualifSlotsRef = useRef<Array<PlayerPair | null>>([]);
  const mainSlotsRef = useRef<Array<PlayerPair | null>>([]);
  const mainDrawSize = (tournament as any)?.config?.mainDrawSize ?? (pairs?.length || 0);
  const matchesCount = Math.max(1, Math.floor((mainDrawSize || 0) / 2));

  const isTwoTeamTournament = mainDrawSize === 2;


  const buildManualRounds = (assignedPairs: Array<PlayerPair | null>, qualifPairs?: Array<PlayerPair | null>, mainPairs?: Array<PlayerPair | null>) => {

    const isQualifKo = (tournament?.config as any)?.format === 'QUALIF_KO';

    if (isQualifKo && qualifPairs && mainPairs) {
      return [
        { stage: 'Q1', games: buildGames(qualifPairs) },
        { stage: getStageFromSize(mainDrawSize), games: buildGames(mainPairs) },
      ];
    }

    return [
      {
        stage: tournament?.rounds?.[0]?.stage ?? undefined as any,
        games: buildGames(assignedPairs),
      },
    ];
  };

  const performDraw = async () => {
    setIsGenerating(true);
    try {
      const isQualifKo = (tournament?.config as any)?.format === 'QUALIF_KO';
      const rounds = buildManualRounds(
        assignedSlotsRef.current,
        isQualifKo ? qualifSlotsRef.current : undefined,
        isQualifKo ? mainSlotsRef.current : undefined
      );
      await generateDraw(tournamentId, rounds);
      const isTwoTeam = mainDrawSize === 2;
      if (isTwoTeam) {
        router.push(`/admin/tournament/${tournamentId}/games?stage=FINAL`);
      } else {
        router.push(`/admin/tournament/${tournamentId}/bracket`);
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const handleDraw = () => {
    confirmAlert({
      title: 'Confirmer le tirage',
      message:
        'Êtes-vous sûr de vouloir générer le tirage ? Cette action créera tous les matchs du premier round.',
      buttons: [
        {
          label: 'Oui',
          onClick: performDraw,
        },
        {
          label: 'Annuler',
          onClick: () => {},
        },
      ],
    });
  };


  useEffect(() => {
    const handleSlotsUpdate = (event: CustomEvent) => {
      if (event.type === 'knockout:pairs-reordered') {
        assignedSlotsRef.current = event.detail.slots;
        setAssignedSlots(event.detail.slots);
      } else if (event.type === 'qualifko:pairs-reordered') {
        qualifSlotsRef.current = event.detail.qualifSlots;
        mainSlotsRef.current = event.detail.mainSlots;
        setQualifSlots(event.detail.qualifSlots);
        setMainSlots(event.detail.mainSlots);
      }
    };

    window.addEventListener('knockout:pairs-reordered', handleSlotsUpdate as EventListener);
    window.addEventListener('qualifko:pairs-reordered', handleSlotsUpdate as EventListener);

    return () => {
      window.removeEventListener('knockout:pairs-reordered', handleSlotsUpdate as EventListener);
      window.removeEventListener('qualifko:pairs-reordered', handleSlotsUpdate as EventListener);
    };
  }, []);

  // Handle save success: generate draw for 2-team tournaments, otherwise switch to assignment tab
  async function handleSaveSuccess(savedPairs?: PlayerPair[]) {
    // Always reload pairs from backend to ensure correct IDs
    const reloadedPairs = await fetchPairs(tournamentId, false, false);
    setPairs(reloadedPairs);

    if (isTwoTeamTournament) {
      // 2 teams: directly generate draw (no assignment needed)
      assignedSlotsRef.current = reloadedPairs.slice(0, 2) as Array<PlayerPair | null>;
      await performDraw();
    } else {
      // Multi-team tournaments: switch to assignment tab
      setActiveTab('assignment');
    }
  }

  const showGenerateButton = !tournamentStarted && !loadingTournament && !loadingPairs && !isTwoTeamTournament && activeTab === 'assignment';

  const isValidPlayerPair = (pair: PlayerPair) => {
    return pair.player1Name && pair.player2Name && pair.type;
  };

  const onPairsChangeRef = useRef<((pairs: PlayerPair[]) => Promise<void>) | undefined>(undefined);

  useEffect(() => {
    onPairsChangeRef.current = async (pairs: PlayerPair[]) => {
      setPairs(pairs);
      if (pairs.length > 0 && pairs.every(isValidPlayerPair)) {
        await savePlayerPairs(tournamentId, pairs);
        await handleSaveSuccess(pairs);
      }
    };
  }, [tournamentId, isTwoTeamTournament]);



  return loadingTournament || !tournament ? (
    <CenteredLoader />
  ) : isTwoTeamTournament ? (
    <AdminTournamentSetupTab2Teams tournamentId={tournamentId} />
  ) : (
    <div className="container mx-auto max-w-3xl">
      {isGenerating && (
        <CenteredLoader />
      )}
      <div className="shadow-sm">
        <section>
          <>
            {!tournamentStarted && !isTwoTeamTournament && (
              <div className="flex justify-center border-b border-border mb-4">
                <button
                  type="button"
                  onClick={() => setActiveTab('players')}
                  className={`px-4 py-2 -mb-px border-b-2 ${activeTab === 'players' ? 'border-primary text-foreground' : 'border-transparent text-tab-inactive'}`}
                >
                  Import
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('assignment')}
                  className={`px-4 py-2 -mb-px border-b-2 ${activeTab === 'assignment' ? 'border-primary text-foreground' : 'border-transparent text-tab-inactive'}`}
                >
                  Affectation
                </button>
              </div>
            )}

            {activeTab === 'players' || tournamentStarted ? (
              tournamentStarted ? (
                <PlayerPairsList tournamentId={tournamentId} pairs={pairs} loading={loadingPairs} editable={true} tournamentStarted={tournamentStarted} />
              ) : (
                <>
                  <h2 className="text-base text-foreground px-2">
                    Lister les joueurs ci-dessous (par ordre de classement ou du tirage)
                  </h2>
                  <div className="flex items-center">
                    <div className="h-px flex-1 bg-border  my-6" />
                    <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">{pairs.length} Equipes inscrites</h3>
                    <div className="h-px flex-1 bg-border" />
                  </div>
                  <p className="p-1 text-tab-inactive"><i>Joueur1,Joueur2,Seed (optionnel)</i></p>
                  <PlayerPairsTextarea
                    onPairsChange={onPairsChangeRef.current}
                    tournamentId={tournamentId}
                    hasStarted={tournamentStarted}
                    onSaveSuccess={handleSaveSuccess}
                  />
                </>
              )
            ) : (
              tournament ? (
                <AdminTournamentPlayerAssignment tournament={tournament} />
              ) : (
                <CenteredLoader />
              )
            )}
          </>
          {showGenerateButton && (
            <>
              <hr className="my-2 border-t border-border" />
              <div className="flex flex-col sm:flex-row sm:justify-center sm:items-end gap-4 mt-4">
                <div className="flex flex-col gap-2 pb-4">
                  <button
                    onClick={pairs.length > 0 ? handleDraw : undefined}
                    disabled={pairs.length === 0}
                    className={`px-4 py-2 h-12 rounded transition-colors ${
                      pairs.length === 0
                        ? 'bg-border text-color-text-secondary cursor-not-allowed'
                        : 'bg-primary text-on-primary hover:bg-primary-hover'
                    }`}
                  >
                    Générer le tirage
                  </button>
                </div>
              </div>
            </>
          )}
        </section>

      </div>

    </div>
  );
}