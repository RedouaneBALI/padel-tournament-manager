//app/admin/tournament/[id]/games/[gameId]/page.tsx
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

export default function AdminGameDetailPage({ params }: PageProps) {
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
  }, [tournamentId, gameId]);

  const handleInfoSaved = (result: { tournamentUpdated: boolean; winner: string | null }) => {
    toast.success('Match mis à jour avec succès !');
    // Recharger le match pour afficher les données à jour
    fetchGame(tournamentId, gameId).then(setGame);
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
      <div className="mb-6 flex justify-between items-center">
        <BackButton />
        <button
          onClick={() => router.push(`/tv/tournament/${tournamentId}/games/${gameId}`)}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors text-sm font-medium"
        >
          📺 Mode TV
        </button>
      </div>

      {game.round?.stage && (
        <div className="flex justify-center mb-6">
          <div className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-primary/10 to-primary/5 border-2 border-primary/20 rounded-full shadow-sm">
            <h1 className="text-xl sm:text-2xl font-bold text-primary">{formatStageLabel(game.round.stage)}</h1>
          </div>
        </div>
      )}

      <div className="flex justify-center w-full">
        <div className="w-full max-w-xl flex justify-center">
          <MatchResultCard
            teamA={game.teamA}
            teamB={game.teamB}
            editable={true}
            gameId={gameId}
            tournamentId={tournamentId}
            score={game.score}
            onInfoSaved={handleInfoSaved}
            onGameUpdated={handleGameUpdated}
            winnerSide={
                              game.finished
                                ? game.winnerSide === 'TEAM_A'
                                  ? 0
                                  : game.winnerSide === 'TEAM_B'
                                    ? 1
                                    : undefined
                                : undefined
                            }
            pool={game.pool}
            finished={game.finished}
            stage={game.round?.stage || game.stage}
            court={game.court}
            scheduledTime={game.scheduledTime}
          />
        </div>
      </div>
    </main>
  );
}
