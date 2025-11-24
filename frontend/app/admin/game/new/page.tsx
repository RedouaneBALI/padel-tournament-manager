'use client';

import React, { useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { Trophy } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { MatchFormat } from '@/src/types/matchFormat';
import { createStandaloneGame } from '@/src/api/tournamentApi';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import BottomNav from '@/src/components/ui/BottomNav';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

export default function NewStandaloneGamePage() {
  const router = useRouter();
  const pathname = usePathname() ?? '';
  const items = getDefaultBottomItems();
  const [isSubmitting, setIsSubmitting] = useState(false);

  // État pour l'équipe A
  const [teamA, setTeamA] = useState<PlayerPair>({
    player1Name: '',
    player2Name: '',
    type: 'NORMAL',
  });

  // État pour l'équipe B
  const [teamB, setTeamB] = useState<PlayerPair>({
    player1Name: '',
    player2Name: '',
    type: 'NORMAL',
  });

  // État pour le format du match
  const [format, setFormat] = useState<MatchFormat>({
    numberOfSetsToWin: 2,
    gamesPerSet: 6,
    tieBreakAt: 6,
    superTieBreakInFinalSet: false,
    advantage: false,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validation
    if (!teamA.player1Name.trim() || !teamA.player2Name.trim()) {
      alert('Veuillez renseigner les noms des joueurs de l\'équipe A');
      return;
    }
    if (!teamB.player1Name.trim() || !teamB.player2Name.trim()) {
      alert('Veuillez renseigner les noms des joueurs de l\'équipe B');
      return;
    }

    setIsSubmitting(true);
    try {
      const game = await createStandaloneGame(teamA, teamB, format);
      // Redirection vers la page du match créé
      router.push(`/admin/game/${game.id}`);
    } catch (error) {
      console.error('Erreur lors de la création du match:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      {isSubmitting && (
        <div className="fixed inset-0 z-[2000] flex items-center justify-center bg-background/60">
          <CenteredLoader size={48} />
        </div>
      )}

      <div
        className="container mx-auto max-w-4xl pb-[calc(var(--bottom-nav-height,64px)+96px)]"
        aria-busy={isSubmitting}
      >
        <div className="bg-card shadow-lg border border-border">
          {/* Header */}
          <div className="p-6 pb-4">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-primary/10">
                <Trophy className="h-6 w-6 text-primary" />
              </div>
              <h1 className="text-2xl font-semibold text-card-foreground">Créer un match unique</h1>
            </div>
          </div>

          <div>
            <form id="game-form" onSubmit={handleSubmit}>
              {/* Section Équipe A */}
              <div className="px-6 py-4">
                <h2 className="text-base font-semibold text-card-foreground mb-3">Équipe A</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input
                    type="text"
                    value={teamA.player1Name}
                    onChange={(e) =>
                      setTeamA({ ...teamA, player1Name: e.target.value })
                    }
                    className="w-full rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="Joueur 1"
                    required
                  />
                  <input
                    type="text"
                    value={teamA.player2Name}
                    onChange={(e) =>
                      setTeamA({ ...teamA, player2Name: e.target.value })
                    }
                    className="w-full rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="Joueur 2"
                    required
                  />
                </div>
              </div>

              <hr className="border-border" />

              {/* Section Équipe B */}
              <div className="px-6 py-4">
                <h2 className="text-base font-semibold text-card-foreground mb-3">Équipe B</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <input
                    type="text"
                    value={teamB.player1Name}
                    onChange={(e) =>
                      setTeamB({ ...teamB, player1Name: e.target.value })
                    }
                    className="w-full rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="Joueur 1"
                    required
                  />
                  <input
                    type="text"
                    value={teamB.player2Name}
                    onChange={(e) =>
                      setTeamB({ ...teamB, player2Name: e.target.value })
                    }
                    className="w-full rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                    placeholder="Joueur 2"
                    required
                  />
                </div>
              </div>

              <hr className="border-border" />

              {/* Section Format du match */}
              <div className="px-6 py-4">
                <MatchFormatForm format={format} onChange={setFormat} />
              </div>
            </form>
          </div>
        </div>

        <ToastContainer
          position="top-right"
          autoClose={5000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
        />
      </div>

      {/* Floating submit button (always visible) */}
      <button
        type="submit"
        form="game-form"
        disabled={isSubmitting}
        className="fixed inset-x-0 z-[1000] flex items-center justify-center gap-2 px-5 py-4 shadow-lg bg-primary text-on-primary hover:bg-primary-hover focus:outline-none focus:ring-2 focus:ring-primary/40"
        style={{
          bottom:
            'calc(env(safe-area-inset-bottom) + var(--bottom-nav-height, 64px) + 12px)',
        }}
        aria-busy={isSubmitting}
        aria-label={isSubmitting ? 'Création en cours…' : 'Créer le match'}
      >
        <Trophy className="h-5 w-5" />
        <span className="text-base font-medium">
          {isSubmitting ? 'Création en cours…' : 'Créer le match'}
        </span>
      </button>

      <BottomNav items={items} pathname={pathname} />
    </>
  );
}

