'use client';

import React, { useEffect, useState } from 'react';
import { Game } from '@/src/types/game';
import { toast } from 'react-toastify';
import { formatStageLabel } from '@/src/types/stage';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';

interface TVModeViewProps {
  gameId: string;
  fetchGameFn: () => Promise<Game>;
  title?: string;
}

/**
 * Composant réutilisable pour afficher un match en mode TV.
 * Peut être utilisé pour les matchs de tournoi ou les matchs standalone.
 */
export default function TVModeView({
  gameId,
  fetchGameFn,
  title,
}: TVModeViewProps) {
  const [game, setGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadGame = async () => {
      try {
        setLoading(true);
        const data = await fetchGameFn();
        setGame(data);
      } catch (err) {
        console.error('Erreur lors du chargement du match:', err);
        toast.error('Erreur lors du chargement du match');
      } finally {
        setLoading(false);
      }
    };

    loadGame();

    // Rafraîchir toutes les 10 secondes pour les mises à jour
    const interval = setInterval(loadGame, 10000);
    return () => clearInterval(interval);
  }, [fetchGameFn]);

  if (loading || !game) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex items-center justify-center">
        <div className="text-white text-4xl">Chargement...</div>
      </div>
    );
  }

  const isInProgress = !game.finished && (game.score?.sets?.some(set => set.teamAScore || set.teamBScore) || false);
  const winnerSide = game.winnerSide ? parseInt(game.winnerSide) : undefined;

  // Calculer les scores par set
  const sets = game.score?.sets || [];
  const visibleSets = Math.max(2, sets.length);

  // Déterminer le titre à afficher
  const displayTitle = title || (game.round?.stage ? formatStageLabel(game.round.stage) : 'Match');

  // Tennis point formatter
  function formatGamePoint(point: import('@/src/types/score').GamePoint | null | undefined) {
    if (point === null || point === undefined) return '';
    switch (point) {
      case 'ZERO': return '0';
      case 'QUINZE': return '15';
      case 'TRENTE': return '30';
      case 'QUARANTE': return '40';
      case 'AVANTAGE': return 'A';
      default: return '';
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex flex-col items-center justify-center p-8 text-white">
      {/* Header avec stage */}
      <div className="mb-12">
        <div className="inline-flex items-center gap-4 px-12 py-6 bg-gradient-to-r from-primary/20 to-primary/10 border-4 border-primary/30 rounded-full">
          {isInProgress && <LiveMatchIndicator showLabel={false} />}
          <h1 className="text-6xl font-bold text-white">{displayTitle}</h1>
        </div>
      </div>

      {/* Match Card - Version TV agrandie */}
      <div className="bg-white rounded-3xl shadow-2xl overflow-hidden tv-mode-width">
        {/* En-tête de la carte */}
        <div className="bg-gradient-to-r from-primary to-primary/80 px-8 py-6 flex justify-between items-center">
          <div className="text-white text-3xl font-semibold">
            {game.pool?.name ? `Poule ${game.pool.name}` : 'Match'}
          </div>
          {isInProgress && (
            <div className="flex items-center gap-3 bg-white/20 px-6 py-3 rounded-full">
              <div className="w-4 h-4 bg-red-500 rounded-full animate-pulse"></div>
              <span className="text-white text-2xl font-bold uppercase">LIVE</span>
            </div>
          )}
        </div>

        {/* Équipes et scores */}
        <div className="divide-y-4 divide-gray-200">
          {/* Team A */}
          <div className={`flex items-center px-8 py-10 ${winnerSide === 0 ? 'bg-green-50' : 'bg-white'}`}>
            <div className="flex-1 min-w-0">
              <div className="text-5xl font-bold text-gray-900 truncate">
                {game.teamA?.player1Name || 'Équipe A'}
              </div>
              <div className="text-5xl font-bold text-gray-900 mt-2 truncate">
                {game.teamA?.player2Name || ''}
              </div>
            </div>

            {/* Scores Team A */}
            <div className="flex gap-8 ml-8 items-center">
              {Array.from({ length: visibleSets }).map((_, i) => (
                <div
                  key={i}
                  className={`w-24 h-24 flex items-center justify-center rounded-xl text-5xl font-bold
                    ${sets[i]?.teamAScore !== null && sets[i]?.teamAScore !== undefined
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-400'
                    }`}
                >
                  {sets[i]?.teamAScore ?? '-'}
                </div>
              ))}
              {/* GamePoint Team A */}
              {(game.score?.currentGamePointA != null || game.score?.currentGamePointB != null) && (
                <div className="w-20 h-20 flex items-center justify-center rounded-xl text-4xl font-bold bg-red-100 text-red-600 ml-4 border-2 border-red-300">
                  {formatGamePoint(game.score?.currentGamePointA ?? 'ZERO')}
                </div>
              )}
            </div>

            {/* Icône champion si gagnant */}
            {winnerSide === 0 && game.finished && (
              <div className="ml-6 text-6xl">🏆</div>
            )}
          </div>

          {/* Team B */}
          <div className={`flex items-center px-8 py-10 ${winnerSide === 1 ? 'bg-green-50' : 'bg-white'}`}>
            <div className="flex-1 min-w-0">
              <div className="text-5xl font-bold text-gray-900 truncate">
                {game.teamB?.player1Name || 'Équipe B'}
              </div>
              <div className="text-5xl font-bold text-gray-900 mt-2 truncate">
                {game.teamB?.player2Name || ''}
              </div>
            </div>

            {/* Scores Team B */}
            <div className="flex gap-8 ml-8 items-center">
              {Array.from({ length: visibleSets }).map((_, i) => (
                <div
                  key={i}
                  className={`w-24 h-24 flex items-center justify-center rounded-xl text-5xl font-bold
                    ${sets[i]?.teamBScore !== null && sets[i]?.teamBScore !== undefined
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-400'
                    }`}
                >
                  {sets[i]?.teamBScore ?? '-'}
                </div>
              ))}
              {/* GamePoint Team B */}
              {(game.score?.currentGamePointA != null || game.score?.currentGamePointB != null) && (
                <div className="w-20 h-20 flex items-center justify-center rounded-xl text-4xl font-bold bg-red-100 text-red-600 ml-4 border-2 border-red-300">
                  {formatGamePoint(game.score?.currentGamePointB ?? 'ZERO')}
                </div>
              )}
            </div>

            {/* Icône champion si gagnant */}
            {winnerSide === 1 && game.finished && (
              <div className="ml-6 text-6xl">🏆</div>
            )}
          </div>
        </div>

        {/* Footer avec court et heure */}
        <div className="bg-gray-100 px-8 py-6 flex justify-between items-center text-3xl text-gray-700">
          <div className="font-semibold">{game.court || 'Court central'}</div>
          <div className="font-semibold">{game.scheduledTime || '00:00'}</div>
        </div>
      </div>

      {/* Footer avec logo/branding */}
      <div className="mt-12 text-white/60 text-2xl">
        www.padelrounds.com
      </div>
    </div>
  );
}
