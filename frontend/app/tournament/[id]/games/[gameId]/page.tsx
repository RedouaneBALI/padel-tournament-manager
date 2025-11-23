'use client';

import React, { use, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { fetchGame } from '@/src/api/tournamentApi';
import { Game } from '@/src/types/game';
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import BackButton from '@/src/components/ui/buttons/BackButton';
import { toast } from 'react-toastify';
import { formatStageLabel } from '@/src/types/stage';

interface PageProps {
  params: Promise<{ id: string; gameId: string }>;
}

export default function GameDetailPage({ params }: PageProps) {
  const { id: tournamentId, gameId } = use(params);
  const router = useRouter();
  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    let intervalId: ReturnType<typeof setInterval> | null = null;

    const loadInitialGame = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchGame(tournamentId, gameId);
        if (cancelled) return;
        setGame(data);

        // Si le match n'est pas terminé, installer l'intervalle de refresh
        if (!data.finished) {
          intervalId = setInterval(async () => {
            try {
              const updated = await fetchGame(tournamentId, gameId);
              if (cancelled) return;
              setGame(updated);

              // Si le match est devenu terminé, on arrête l'intervalle
              if (updated.finished && intervalId) {
                clearInterval(intervalId);
                intervalId = null;
              }
            } catch (e) {
              // ignore errors for periodic refresh (optional: log)
              console.error('Refresh error:', e);
            }
          }, 30000);
        }
      } catch (err) {
        console.error('Erreur lors du chargement du match:', err);
        if (!cancelled) {
          setError('Impossible de charger le match');
          toast.error('Erreur lors du chargement du match');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    loadInitialGame();

    return () => {
      cancelled = true;
      if (intervalId) clearInterval(intervalId);
    };
  }, [tournamentId, gameId]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Chargement du match...</p>
        </div>
      </div>
    );
  }

  if (error || !game) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-destructive mb-4">{error || 'Match introuvable'}</p>
          <BackButton />
        </div>
      </div>
    );
  }

  // Indicateur de mise à jour automatique (affiché seulement si le match peut s'actualiser)
  const showAutoRefresh = !!game && !game.finished;

  return (
    <main className="px-4 sm:px-6 py-4 pb-24 min-h-screen">
      <div className="mb-6">
        <BackButton />
      </div>

      {game.round?.stage && (
        <div className="flex justify-center mb-8">
          <div className="inline-flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-primary/10 to-primary/5 border-2 border-primary/20 rounded-full shadow-sm">
            <h1 className="text-3xl sm:text-4xl font-bold text-primary">{formatStageLabel(game.round.stage)}</h1>
          </div>
        </div>
      )}

      <div className="flex justify-center w-full">
        <div className="w-full max-w-2xl flex justify-center transform scale-110 sm:scale-125">
          <MatchResultCard
            teamA={game.teamA}
            teamB={game.teamB}
            editable={false}
            gameId={gameId}
            tournamentId={tournamentId}
            score={game.score}
            winnerSide={game.winnerSide ? parseInt(game.winnerSide) : undefined}
            pool={game.pool}
            finished={game.finished}
            stage={game.round?.stage || game.stage}
            court={game.court}
            scheduledTime={game.scheduledTime}
          />
        </div>
      </div>

      {showAutoRefresh ? (
        <div className="mt-8 text-center text-sm text-muted-foreground">
          ⟳ Actualisation automatique toutes les 30 secondes
        </div>
      ) : (
        <div className="mt-8 text-center text-sm text-muted-foreground">
          {game?.finished ? 'Match terminé — actualisation désactivée' : ''}
        </div>
      )}
    </main>
  );
}
