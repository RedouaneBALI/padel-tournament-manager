'use client';

import React, { useEffect, useState, useContext } from 'react';
import { Game } from '@/src/types/game';
import { Score } from '@/src/types/score';
import MatchResultCardZoom from '@/src/components/match/MatchResultCardZoom';
import MatchShareCard from '@/src/components/match/MatchShareCard';
import VoteModule from '@/src/components/match/VoteModule';
import { toast } from 'react-toastify';
import { formatStageLabel } from '@/src/types/stage';
import { TournamentContext } from '@/src/contexts/TournamentContext';
import { Share } from 'lucide-react';
import { shareMatchImage } from '@/src/utils/imageExport';
import { TeamSide } from '@/src/types/teamSide';

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
  const displayTitle = contextTournament?.name || title || undefined;
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

  const handleInfoSaved = (result: { tournamentUpdated: boolean; winner: TeamSide | null }) => {
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

    const fileName = `match-${title || 'tournament'}-${game.round?.stage ? formatStageLabel(game.round.stage) : 'round'}.png`;
    const customTitle = `Match ${title || ''} - ${game.round?.stage ? formatStageLabel(game.round.stage) : ''}`;
    const customText = `Découvrez le score du match sur PadelRounds !`;

    await shareMatchImage(game, displayTitle, displayClub, fileName, customTitle, customText);
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
