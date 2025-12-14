'use client';

import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { useRouter } from 'next/navigation';
import { fetchTournament, fetchPairs, savePlayerPairs, generateDraw } from '@/src/api/tournamentApi';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { hasTournamentStarted } from '@/src/utils/tournamentUtils';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import { getStageFromSize } from '@/src/types/stage';
import { PairType } from '@/src/types/pairType';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentSetupTab2Teams({ tournamentId }: Props) {
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [tournamentStarted, setTournamentStarted] = useState(false);
  const [loadingTournament, setLoadingTournament] = useState(true);

  const [teamA, setTeamA] = useState<PlayerPair>({
    player1Name: '',
    player2Name: '',
    type: 'NORMAL',
  });

  const [teamB, setTeamB] = useState<PlayerPair>({
    player1Name: '',
    player2Name: '',
    type: 'NORMAL',
  });

  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const router = useRouter();

  useEffect(() => {
    const loadTournament = async () => {
      setLoadingTournament(true);
      try {
        const data = await fetchTournament(tournamentId);
        setTournament(data);
        setTournamentStarted(hasTournamentStarted(data));
      } catch (e: any) {
        toast.error('Erreur lors du chargement du tournoi.');
      } finally {
        setLoadingTournament(false);
      }
    };
    loadTournament();
  }, [tournamentId]);

  useEffect(() => {
    const loadPairs = async () => {
      try {
        const data = await fetchPairs(tournamentId, false, false);
        if (data.length >= 1) {
          setTeamA(data[0]);
        }
        if (data.length >= 2) {
          setTeamB(data[1]);
        }
      } catch (e: any) {
        toast.error(e?.message ?? 'Impossible de charger les équipes.');
      }
    };
    loadPairs();
  }, [tournamentId]);

  const handleTeamAChange = (player1: string, player2: string) => {
    setTeamA({
      ...teamA,
      player1Name: player1,
      player2Name: player2,
    });
  };

  const handleTeamBChange = (player1: string, player2: string) => {
    setTeamB({
      ...teamB,
      player1Name: player1,
      player2Name: player2,
    });
  };

  const isValidTeam = (team: PlayerPair): boolean =>
    team.player1Name.trim() !== '' && team.player2Name.trim() !== '';

  const handleClear = () => {
    setTeamA({ player1Name: '', player2Name: '', type: 'NORMAL' });
    setTeamB({ player1Name: '', player2Name: '', type: 'NORMAL' });
  };

  const performDrawWithPairs = async (pairA: PlayerPair, pairB: PlayerPair) => {
    if (!pairA?.id || !pairB?.id) {
      toast.error('Les équipes n\'ont pas d\'ID. Veuillez réessayer.');
      return;
    }

    setIsGenerating(true);
    try {
      const rounds = [
        {
          stage: tournament?.rounds?.[0]?.stage ?? getStageFromSize(2),
          games: [
            {
              teamA: { type: 'NORMAL' as PairType, pairId: pairA.id },
              teamB: { type: 'NORMAL' as PairType, pairId: pairB.id },
            },
          ],
        },
      ];
      await generateDraw(tournamentId, rounds);
      router.push(`/admin/tournament/${tournamentId}/games?stage=FINAL`);
    } catch (e: any) {
      toast.error(e?.message ?? 'Erreur lors de la génération du tirage.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSave = async () => {
    if (!isValidTeam(teamA) || !isValidTeam(teamB)) {
      toast.error('Veuillez remplir tous les champs.');
      return;
    }

    setIsSaving(true);
    try {
      await savePlayerPairs(tournamentId, [teamA, teamB]);
      toast.success('Équipes enregistrées.');

      await new Promise(resolve => setTimeout(resolve, 500));

      const reloadedPairs = await fetchPairs(tournamentId, false, false);
      if (reloadedPairs.length >= 2) {
        await performDrawWithPairs(reloadedPairs[0], reloadedPairs[1]);
      }
    } catch (e: any) {
      console.error('[AdminTournamentSetupTab2Teams] Error saving:', e);
      toast.error(e?.message ?? 'Erreur lors de l\'enregistrement.');
    } finally {
      setIsSaving(false);
    }
  };

  if (loadingTournament) {
    return <CenteredLoader />;
  }

  return (
    <div className="container mx-auto max-w-3xl">
      {isGenerating && <CenteredLoader />}

      <div className="shadow-sm">
        <section className="bg-card">
          <h2 className="text-base text-foreground px-4 pt-4">Match</h2>

          <div className="p-4 space-y-6">
            <div className="border border-border rounded-lg p-4">
              <h3 className="text-sm font-semibold text-foreground mb-3">Équipe A</h3>
              <div className="space-y-3">
                <div>
                  <input
                    type="text"
                    value={teamA.player1Name}
                    onChange={e => handleTeamAChange(e.target.value, teamA.player2Name)}
                    disabled={tournamentStarted}
                    className="w-full px-3 py-2 border border-border rounded bg-background text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="Nom du joueur 1"
                  />
                </div>
                <div>
                  <input
                    type="text"
                    value={teamA.player2Name}
                    onChange={e => handleTeamAChange(teamA.player1Name, e.target.value)}
                    disabled={tournamentStarted}
                    className="w-full px-3 py-2 border border-border rounded bg-background text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="Nom du joueur 2"
                  />
                </div>
              </div>
            </div>

            <div className="border border-border rounded-lg p-4">
              <h3 className="text-sm font-semibold text-foreground mb-3">Équipe B</h3>
              <div className="space-y-3">
                <div>
                  <input
                    type="text"
                    value={teamB.player1Name}
                    onChange={e => handleTeamBChange(e.target.value, teamB.player2Name)}
                    disabled={tournamentStarted}
                    className="w-full px-3 py-2 border border-border rounded bg-background text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="Nom du joueur 1"
                  />
                </div>
                <div>
                  <input
                    type="text"
                    value={teamB.player2Name}
                    onChange={e => handleTeamBChange(teamB.player1Name, e.target.value)}
                    disabled={tournamentStarted}
                    className="w-full px-3 py-2 border border-border rounded bg-background text-foreground disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="Nom du joueur 2"
                  />
                </div>
              </div>
            </div>
          </div>

          {!tournamentStarted && (
            <>
              <hr className="my-4 border-t border-border" />
              <div className="flex justify-center gap-4 px-4 pb-4">
                <button
                  onClick={handleClear}
                  className="px-4 py-2 border border-border text-foreground rounded hover:bg-background"
                >
                  Effacer
                </button>
                <button
                  onClick={handleSave}
                  disabled={isSaving}
                  className="px-4 py-2 bg-primary text-on-primary rounded hover:bg-primary-hover disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isSaving ? 'Enregistrement...' : 'Enregistrer et générer'}
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
