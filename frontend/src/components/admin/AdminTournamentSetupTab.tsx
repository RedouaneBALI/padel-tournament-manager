'use client';

import React, { useEffect, useState, useRef } from 'react';
import { ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/src/components/tournament/players/PlayerPairsTextarea';
import PlayerPairsList from '@/src/components/tournament/players/PlayerPairsList';
import { useRouter } from 'next/navigation';
import { confirmAlert } from 'react-confirm-alert';
import { generateDraw, initializeDraw, savePlayerPairs } from '@/src/api/tournamentApi';
import type { InitializeDrawRequest } from '@/src/types/api/InitializeDrawRequest';
import { FileText } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { fetchTournament, fetchPairs } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import AdminTournamentPlayerAssignment from '@/src/components/admin/AdminTournamentPlayerAssignment';
import { getStageFromSize } from '@/src/types/stage';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentSetupTab({ tournamentId }: Props) {
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
  const [loadingPairs, setLoadingPairs] = useState(true);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [tournamentStarted, setTournamentStarted] = useState(false);
  const [loadingTournament, setLoadingTournament] = useState(true);

  const [isGenerating, setIsGenerating] = useState(false);
  const router = useRouter();

  const [activeTab, setActiveTab] = useState<'players' | 'assignment'>('players');

  const [assignedSlots, setAssignedSlots] = useState<Array<PlayerPair | null>>([]);
  const [qualifSlots, setQualifSlots] = useState<Array<PlayerPair | null>>([]);
  const [mainSlots, setMainSlots] = useState<Array<PlayerPair | null>>([]);
  const assignedSlotsRef = useRef<Array<PlayerPair | null>>([]);
  const qualifSlotsRef = useRef<Array<PlayerPair | null>>([]);
  const mainSlotsRef = useRef<Array<PlayerPair | null>>([]);
  const mainDrawSize = (tournament as any)?.config?.mainDrawSize ?? (pairs?.length || 0);
  const matchesCount = Math.max(1, Math.floor((mainDrawSize || 0) / 2));

  const manual = (tournament?.config as any)?.drawMode === 'MANUAL';
  const handleDraw = () => {
    confirmAlert({
      title: 'Confirmer le tirage',
      message:
        'Êtes-vous sûr de vouloir générer le tirage ? Cette action créera tous les matchs du premier round.',
      buttons: [
        {
          label: 'Oui',
          onClick: async () => {
            setIsGenerating(true);
            try {
              const manual = (tournament?.config as any)?.drawMode === 'MANUAL';
              if (manual) {
                const isQualifKo = (tournament?.config as any)?.format === 'QUALIF_KO';

                if (isQualifKo) {
                  // Build two rounds: qualif and main
                  console.log('QUALIF_KO mode: qualifSlotsRef.current:', qualifSlotsRef.current);
                  console.log('QUALIF_KO mode: mainSlotsRef.current:', mainSlotsRef.current);

                  const toTeamSlot = (p: PlayerPair | null) => {
                    if (!p || !p.id) return { type: 'BYE' as const };
                    return { type: 'NORMAL' as const, pairId: p.id };
                  };

                  const buildGames = (slots: (PlayerPair | null)[]) => {
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
                  };

                  const rounds = [
                    {
                      stage: 'Q1',
                      games: buildGames(qualifSlotsRef.current),
                    },
                    {
                      stage: getStageFromSize(mainDrawSize),
                      games: buildGames(mainSlotsRef.current),
                    },
                  ];

                  console.log('QUALIF_KO rounds to send:', rounds);

                  await generateDraw(tournamentId, { mode: 'manual', rounds });
                } else {
                  // Build a single Round from the current assignment slots
                  console.log('Classic mode: assignedSlotsRef.current:', assignedSlotsRef.current);

                  const size = matchesCount * 2;
                  const slots = assignedSlotsRef.current;

                  const games = Array.from({ length: matchesCount }).map((_, m) => {
                    const iA = m * 2;
                    const iB = iA + 1;
                    const a = slots[iA];
                    const b = slots[iB];

                    const toTeamSlot = (p: PlayerPair | null) => {
                      if (!p || !p.id) return { type: 'BYE' as const };
                      return { type: 'NORMAL' as const, pairId: p.id };
                    };

                    return {
                      teamA: toTeamSlot(a),
                      teamB: toTeamSlot(b),
                    };
                  });

                  const rounds = [
                    {
                      stage: tournament?.rounds?.[0]?.stage ?? undefined as any,
                      games,
                    },
                  ];

                  console.log('Classic rounds to send:', rounds);

                  await generateDraw(tournamentId, { mode: 'manual', rounds });
                }
              } else {
                await generateDraw(tournamentId, { mode: 'auto' });
              }
              router.push(`/admin/tournament/${tournamentId}/bracket`);
            } finally {
              setIsGenerating(false);
            }
          },
        },
        {
          label: 'Annuler',
          onClick: () => {},
        },
      ],
    });
  };

  useEffect(() => {
    async function loadTournament() {
      setLoadingTournament(true);
      try {
        const data = await fetchTournament(tournamentId);
        setTournament(data);
        const hasStarted = !!data.rounds?.some(round =>
          round.games?.some(game => game.score !== null)
        );
        setTournamentStarted(hasStarted);
        if (hasStarted) {
          setActiveTab('players');
        }
      } finally {
        setLoadingTournament(false);
      }
    }
    loadTournament();
  }, [tournamentId]);

  useEffect(() => {
    let cancelled = false;
    async function loadPairs() {
      setLoadingPairs(true);
      try {
        const data = await fetchPairs(tournamentId, false, false);
        if (!cancelled) setPairs(data);
      } finally {
        if (!cancelled) setLoadingPairs(false);
      }
    }
    loadPairs();
    return () => {
      cancelled = true;
    };
  }, [tournamentId]);

  useEffect(() => {
    const onReorder = (e: any) => {
      if (!e?.detail) return;
      // Optionally, check tournament id
      assignedSlotsRef.current = e.detail.slots || [];
      setAssignedSlots(e.detail.slots || []);
    };
    if (typeof window !== 'undefined') {
      window.addEventListener('knockout:pairs-reordered', onReorder as any);
    }
    return () => {
      if (typeof window !== 'undefined') {
        window.removeEventListener('knockout:pairs-reordered', onReorder as any);
      }
    };
  }, []);


  // Listen to slots updates
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

  // Handle save success: only switch to assignment tab
  async function handleSaveSuccess() {
    setActiveTab('assignment');
  }

  const showGenerateButton = !tournamentStarted && !loadingTournament && !loadingPairs && (
    (!manual) || (manual && activeTab === 'assignment')
  );

  const isValidPlayerPair = (pair: PlayerPair) => {
    return pair.player1Name && pair.player2Name && pair.type;
  };

  const onPairsChangeRef = useRef<typeof setPairs | null>(null);

  useEffect(() => {
    onPairsChangeRef.current = async (pairs: PlayerPair[]) => {
      setPairs(pairs);
      // Ne sauvegarde que si toutes les paires sont valides et non vides
      if (pairs.length > 0 && pairs.every(isValidPlayerPair)) {
        console.log("Save from AdminTournamentSetupTab.tsx");
        await savePlayerPairs(tournamentId, pairs);
      }
    };
  }, [tournamentId]);


  return (
    <div className="container mx-auto max-w-3xl">
      {isGenerating && (
        <CenteredLoader />
      )}
      <div className="shadow-sm">
        <section>
          {loadingTournament ? (
            <CenteredLoader />
          ) : !manual ? (
            // --- MODE AUTO: on garde l'affichage actuel ---
            tournamentStarted ? (
              <PlayerPairsList tournamentId={tournamentId} pairs={pairs} loading={loadingPairs} editable={true} />
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
                  onPairsChange={setPairs}
                  tournamentId={tournamentId}
                  hasStarted={tournamentStarted}
                />
              </>
            )
          ) : (
            // --- MODE MANUEL: tabs Joueurs / Affectation ---
            <>
              {!tournamentStarted && (
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
                  <PlayerPairsList tournamentId={tournamentId} pairs={pairs} loading={loadingPairs} editable={true} />
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
          )}
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