'use client';

import React, { useEffect, useState } from 'react';
import { Game } from '@/src/types/game';
import { Score } from '@/src/types/score';
import MatchResultCardZoom from '@/src/components/match/MatchResultCardZoom';
import BackButton from '@/src/components/ui/buttons/BackButton';
import { toast } from 'react-toastify';
import { formatStageLabel } from '@/src/types/stage';

interface GameDetailViewProps {
  gameId: string;
  tournamentId?: string;
  fetchGameFn: () => Promise<Game>;
  updateGameFn?: (gameId: string, scorePayload: Score, court: string, scheduledTime: string) => Promise<any>;
  editable?: boolean;
  title?: string;
}

/**
 * Composant réutilisable pour afficher les détails d'un match.
 * Peut être utilisé pour les matchs de tournoi ou les matchs standalone.
 */
export default function GameDetailView({
  gameId,
  tournamentId,
  fetchGameFn,
  updateGameFn,
  editable = false,
  title,
}: GameDetailViewProps) {
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
        const data = await fetchGameFn();
        if (cancelled) return;
        setGame(data);

        // Si le match n'est pas terminé, installer l'intervalle de refresh
        if (!data.finished) {
          intervalId = setInterval(async () => {
            try {
              const updated = await fetchGameFn();
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
  }, [fetchGameFn]);

  const handleInfoSaved = (result: { tournamentUpdated: boolean; winner: string | null }) => {
    toast.success('Match mis à jour avec succès !');
    // Recharger le match pour afficher les données à jour
    fetchGameFn().then(setGame);
  };

  const handleGameUpdated = (updatedGameId: string, changes: { scheduledTime?: string; court?: string }) => {
    if (game) {
      setGame({
        ...game,
        scheduledTime: changes.scheduledTime || game.scheduledTime,
        court: changes.court || game.court,
      });
    }
  };

  if (loading) {
    return (
      <div className="min-h-full flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Chargement du match...</p>
        </div>
      </div>
    );
  }

  if (error || !game) {
    return (
      <div className="min-h-full flex items-center justify-center">
        <div className="text-center">
          <p className="text-destructive mb-4">{error || 'Match introuvable'}</p>
          <BackButton />
        </div>
      </div>
    );
  }

  return (
    <main className="px-4 sm:px-6 py-4 pb-12 min-h-full">
      <div className="mb-6 flex justify-between items-center">
        <BackButton />
      </div>

      {title && (
        <div className="flex justify-center mb-6">
          <div className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-primary/10 to-primary/5 border-2 border-primary/20 rounded-full shadow-sm">
            <h1 className="text-xl sm:text-2xl font-bold text-primary">{title}</h1>
          </div>
        </div>
      )}

      {!title && game.round?.stage && (
        <div className="flex justify-center mb-6">
          <div className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-primary/10 to-primary/5 border-2 border-primary/20 rounded-full shadow-sm">
            <h1 className="text-xl sm:text-2xl font-bold text-primary">{formatStageLabel(game.round.stage)}</h1>
          </div>
        </div>
      )}

      <div className="flex justify-center w-full">
        <div className="w-full max-w-xl flex justify-center">
          <MatchResultCardZoom
            game={game}
            tournamentId={tournamentId || game.tournamentId || ''}
            mode={editable ? 'admin' : 'spectator'}
            onScoreUpdate={(updatedGame) => setGame(updatedGame)}
          />
        </div>
      </div>
    </main>
  );
}
