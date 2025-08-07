'use client';

import React, { useEffect, useState } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/src/components/tournament/PlayerPairsTextarea';
import PlayerPairList from '@/src/components/tournament/PlayerPairsList';
import { useRouter } from 'next/navigation';
import { confirmAlert } from 'react-confirm-alert';
import { generateDraw } from '@/src/api/tournamentApi';
import { FileText } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { fetchTournament, fetchPairs } from '@/src/api/tournamentApi';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentSetupTab({ tournamentId }: Props) {
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [tournamentStarted, setTournamentStarted] = useState(false);

  const [drawMode, setDrawMode] = useState('seeded');
  const router = useRouter();

  const handleDraw = () => {
    confirmAlert({
      title: 'Confirmer le tirage',
      message:
        'Êtes-vous sûr de vouloir générer le tirage ? Cette action créera tous les matchs du premier round.',
      buttons: [
        {
          label: 'Oui',
          onClick: async () => {
            try {
              const manual = drawMode === 'order';
              await generateDraw(tournamentId, manual);
              toast.success("Tirage généré !");
              router.push(`/admin/tournament/${tournamentId}/rounds/results`);
            } catch {
              toast.error("Tirage déjà effectué ou erreur serveur.");
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
      try {
        const data = await fetchTournament(tournamentId);
        setTournament(data);
        const hasStarted = data.rounds?.some(round =>
          round.games?.some(game => game.score !== null)
        );
        setTournamentStarted(hasStarted);
      } catch {
        toast.error('Erreur réseau lors de la récupération du tournoi.');
      }
    }
    loadTournament();
  }, [tournamentId]);

  useEffect(() => {
    async function loadPairs() {
      try {
        const data = await fetchPairs(tournamentId);
        setPairs(data);
      } catch {
        toast.error('Erreur réseau lors de la récupération des joueurs.');
      }
    }
    loadPairs();
  }, [tournamentId]);


  return (
    <div className="container mx-auto max-w-3xl">
      <div className="bg-card shadow-sm">
        <div className="flex items-center gap-2 border-b border-border pb-3">
          <FileText className="h-5 w-5 text-muted-foreground" />
          <h1 className="text-xl font-semibold text-foreground">
            {pairs.length} paires
          </h1>
        </div>

        <section>
          {tournamentStarted ? (
            <PlayerPairList pairs={pairs} />
          ) : (
            <>
              <h2 className="mb-3 text-base font-semibold text-foreground">
                Lister les joueurs ci-dessous (par ordre de classement ou du tirage)
              </h2>
              <PlayerPairsTextarea
                onPairsChange={setPairs}
                tournamentId={tournamentId}
                hasStarted={tournamentStarted}
              />
            </>
          )}
          {pairs.length > 0 && !tournamentStarted && (
            <>
              <hr className="my-6 border-t border-gray-300" />
              <div className="flex flex-col sm:flex-row sm:justify-center sm:items-end gap-4 mt-6">
                <div className="flex flex-col gap-2">
                  <label className="text-xl font-semibold text-foreground text-center">Tirage</label>
                  <select
                    onChange={(e) => setDrawMode(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md text-sm h-10 shadow-sm text-center"
                    value={drawMode}
                  >
                    <option value="seeded">Par classement (TS)</option>
                    <option value="order">Par ordre d&apos;enregistrement</option>
                  </select>
                  <button
                    onClick={handleDraw}
                    className="px-4 py-2 bg-[#1b2d5e] text-white rounded hover:bg-blue-900 h-10"
                  >
                    Générer le tirage
                  </button>
                </div>
              </div>
            </>
          )}
        </section>

      </div>

      <ToastContainer />
    </div>
  );
}