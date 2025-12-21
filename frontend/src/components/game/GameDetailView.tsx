'use client';

import React, { useEffect, useState, useContext } from 'react';
import { Game } from '@/src/types/game';
import { Score } from '@/src/types/score';
import MatchResultCardZoom from '@/src/components/match/MatchResultCardZoom';
import MatchShareCard from '@/src/components/match/MatchShareCard';
import VoteModule from '@/src/components/match/VoteModule';
import { toast } from 'react-toastify';
import { formatStageLabel } from '@/src/types/stage';
import { toBlob } from 'html-to-image';
import { TournamentContext } from '@/src/contexts/TournamentContext';
import { Share } from 'lucide-react';

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
  const contextTournament = useContext(TournamentContext);
  const displayTitle = title || contextTournament?.name || undefined;
  const displayClub = contextTournament?.club || undefined;

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

  const hasMatchStarted = (score: Score | null) => {
    if (!score) return false;

    // Vérifier si des sets ont des scores > 0
    if (score.sets && score.sets.some(set => (set.teamAScore !== null && set.teamAScore > 0) || (set.teamBScore !== null && set.teamBScore > 0))) return true;

    // Vérifier si les points actuels ne sont pas à zéro
    if (score.currentGamePointA && score.currentGamePointA !== 'ZERO') return true;
    if (score.currentGamePointB && score.currentGamePointB !== 'ZERO') return true;

    // Vérifier les points de tie-break
    if (score.tieBreakPointA && score.tieBreakPointA > 0) return true;
    if (score.tieBreakPointB && score.tieBreakPointB > 0) return true;

    return false;
  };

  const handleShare = async () => {
    if (!game) return;

    let tempContainer: HTMLDivElement | null = null;

    try {
      // Create a temporary container off-DOM
      tempContainer = document.createElement('div');
      tempContainer.style.position = 'fixed';
      tempContainer.style.top = '-9999px';
      tempContainer.style.left = '-9999px';
      tempContainer.style.zIndex = '-9999';
      tempContainer.style.opacity = '0';
      tempContainer.style.pointerEvents = 'none';
      tempContainer.style.padding = '0';
      tempContainer.style.margin = '0';
      tempContainer.style.border = 'none';
      tempContainer.style.overflow = 'visible';

      // Add to DOM before rendering
      document.body.appendChild(tempContainer);

      // Render the component into this container
      const { createRoot } = await import('react-dom/client');
      const root = createRoot(tempContainer);
      root.render(
        <MatchShareCard game={game} tournamentName={displayTitle} club={displayClub} />
      );

      // Wait for render to complete and force layout recalculation
      await new Promise(resolve => setTimeout(resolve, 100));

      // Trigger a reflow to ensure layout is calculated
      tempContainer.offsetHeight;

      // Capture the element
      const shareElement = tempContainer.querySelector('.match-share-card') as HTMLElement;
      if (!shareElement) throw new Error('Élément non trouvé');

      // Force another reflow
      shareElement.offsetHeight;

      // Use scrollHeight for actual content, scrollWidth for actual width
      const elementWidth = shareElement.scrollWidth;
      const elementHeight = shareElement.scrollHeight;

      // Get the primary color for background
      const primaryColor = getComputedStyle(document.documentElement).getPropertyValue('--color-primary').trim() || '#3b82f6';

      const blob = await toBlob(shareElement, {
        cacheBust: true,
        backgroundColor: primaryColor,
        width: elementWidth,
        height: elementHeight,
        pixelRatio: 2,
      });

      // Cleanup
      root.unmount();
      if (tempContainer.parentNode) {
        document.body.removeChild(tempContainer);
      }

      if (!blob) throw new Error('Échec de la génération de l\'image');

      const fileName = `match-${title || 'tournament'}-${game.round?.stage ? formatStageLabel(game.round.stage) : 'round'}.png`;
      const file = new File([blob], fileName, { type: 'image/png' });

      const shareData = {
        title: `Match ${title || ''} - ${game.round?.stage ? formatStageLabel(game.round.stage) : ''}`,
        text: `Découvrez le score du match sur PadelRounds !`,
        files: [file],
      };

      if (navigator.canShare && navigator.canShare(shareData)) {
        try {
          await navigator.share(shareData);
          return;
        } catch (err) {
          if ((err as Error).name !== 'AbortError') console.warn('Erreur partage:', err);
        }
      }

      // Fallback: download the image
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

    } catch (error) {
      // Cleanup in case of error
      if (tempContainer?.parentNode) {
        try {
          document.body.removeChild(tempContainer);
        } catch (e) {
          console.warn('Erreur cleanup:', e);
        }
      }
      console.error('Erreur lors de la création de l\'image:', error);
      toast.error('Impossible de générer l\'image pour le moment.');
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
        </div>
      </div>
    );
  }

  const totalVotes = game.votes ? game.votes.teamAVotes + game.votes.teamBVotes : 0;

  return (
    <main className={`${editable ? '' : 'px-4 sm:px-6'}`}>
      {/* Ligne 2 : nom du tournoi (et éventuellement boutons admin) */}
      <div className="mb-2 flex items-center gap-2">
        {title && (
          <span className="text-lg sm:text-xl font-bold text-primary truncate">{title}</span>
        )}
        {/* Place ici les boutons admin si besoin, par exemple : */}
        {/* {isAdmin && <AdminActions ... />} */}
      </div>

      {/* Ligne 3 : stage name centré */}
      {game.round?.stage && (
        <div className="flex justify-center mb-4">
          <span className="px-3 py-1 rounded-full bg-primary/10 text-primary text-base font-semibold">
            {formatStageLabel(game.round.stage)}
          </span>
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
      {/* Afficher VoteModule seulement si (match non démarré) OU (match démarré et au moins un vote) */}
      {(!hasMatchStarted(game.score ?? null) || totalVotes > 0) && (
        <VoteModule
          gameId={game.id}
          isVotingDisabled={hasMatchStarted(game.score ?? null)}
          votes={game.votes}
        />
      )}
      {/* Composant de partage */}
      <div className="hidden">
        <MatchShareCard game={game} tournamentName={displayTitle} club={displayClub} />
      </div>
      {/* Bouton de partage */}
      <div className="mt-4 flex justify-center">
        <button
          onClick={handleShare}
          className="px-4 py-2 bg-primary text-white rounded-md shadow-md hover:bg-primary/90 transition-colors"
        >
          <Share className="w-5 h-5 inline-block mr-2" />
          Partager le match
        </button>
      </div>
    </main>
  );
}
