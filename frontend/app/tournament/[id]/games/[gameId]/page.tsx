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
    const loadGame = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchGame(tournamentId, gameId);
        setGame(data);
      } catch (err) {
        console.error('Erreur lors du chargement du match:', err);
        setError('Impossible de charger le match');
        toast.error('Erreur lors du chargement du match');
      } finally {
        setLoading(false);
      }
    };

    loadGame();

    // Auto-refresh toutes les 5 secondes pour affichage en direct
    const interval = setInterval(() => {
      loadGame();
    }, 5000);

    return () => clearInterval(interval);
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

      {/* Indicateur de mise à jour automatique */}
      <div className="mt-8 text-center text-sm text-muted-foreground">
        ⟳ Actualisation automatique toutes les 5 secondes
      </div>
    </main>
  );
}

