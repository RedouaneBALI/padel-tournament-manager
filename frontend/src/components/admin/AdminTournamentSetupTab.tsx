'use client';

import React, { useEffect, useState } from 'react';
import { ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/src/components/tournament/players/PlayerPairsTextarea';
import PlayerPairsList from '@/src/components/tournament/players/PlayerPairsList';
import { useRouter } from 'next/navigation';
import { confirmAlert } from 'react-confirm-alert';
import { generateDraw } from '@/src/api/tournamentApi';
import { FileText } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { fetchTournament, fetchPairs } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentSetupTab({ tournamentId }: Props) {
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
  const [loadingPairs, setLoadingPairs] = useState(true);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [tournamentStarted, setTournamentStarted] = useState(false);
  const [loadingTournament, setLoadingTournament] = useState(true);

  const [drawMode, setDrawMode] = useState('seeded');
  const [isGenerating, setIsGenerating] = useState(false);
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
            setIsGenerating(true);
            try {
              const manual = drawMode === 'order';
              await generateDraw(tournamentId, manual);
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
        const data = await fetchPairs(tournamentId);
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


  return (
    <div className="container mx-auto max-w-3xl">
      {isGenerating && (
        <CenteredLoader />
      )}
      <div className="shadow-sm">
        <section>
          {loadingTournament ? (
            <CenteredLoader />
          ) : tournamentStarted ? (
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
          )}
          {pairs.length > 0 && !tournamentStarted && !loadingTournament && !loadingPairs && (
            <>
              <hr className="my-2 border-t border-border" />
              <div className="flex flex-col sm:flex-row sm:justify-center sm:items-end gap-4 mt-4">
                <div className="flex flex-col gap-2 pb-4">
                  <label className="text-xl font-semibold text-foreground text-center">Tirage</label>
                  <select
                    onChange={(e) => setDrawMode(e.target.value)}
                    className="px-3 py-2 h-12 border border-border rounded-md text-sm h-10 shadow-sm text-center"
                    value={drawMode}
                  >
                    <option value="seeded">{tournament?.config.nbSeeds === 0 ? 'Aléatoire' : 'Par classement (TS)'}</option>
                    <option value="order">Par ordre d&apos;enregistrement</option>
                  </select>
                  <button
                    onClick={handleDraw}
                    className="px-4 py-2 h-12 bg-primary text-on-primary rounded hover:bg-primary-hover"
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