'use client';

import React, { useEffect, useState } from 'react';
import { Game } from '@/src/types/game';
import { Score } from '@/src/types/score';
import MatchResultCardZoom from '@/src/components/match/MatchResultCardZoom';
import VoteModule from '@/src/components/match/VoteModule';
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

    const loadInitialGame = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchGameFn();
        if (cancelled) return;
        setGame(data);
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
    <main className={`py-4 pb-12 min-h-full ${editable ? '' : 'px-4 sm:px-6'}`}>
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
        <div className="w-full">
          <MatchResultCardZoom
            game={game}
            tournamentId={tournamentId || game.tournamentId || ''}
            editable={editable}
            onScoreUpdate={(updatedGame) => setGame(updatedGame)}
          />
        </div>
      </div>

      <VoteModule gameId={game.id} isVotingDisabled={!!game.score} />
    </main>
  );
}
