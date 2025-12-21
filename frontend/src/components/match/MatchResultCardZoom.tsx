import React, { useEffect, useState } from 'react';
import type { Game, TeamSide } from '@/src/types/game';
import type { Score } from '@/src/types/score';
import type { PlayerPair } from '@/src/types/playerPair';
import type { MatchFormat } from '@/src/types/matchFormat';
import { incrementGamePoint, undoGamePoint, fetchMatchFormat } from '@/src/api/tournamentApi';
import { Undo2, MapPin, Clock, Trophy, Loader2 } from 'lucide-react';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';
import { useRealtimeGame } from '@/src/hooks/useRealtimeGame';
import { processSuperTieBreakScore } from '@/src/utils/scoreUtils';
import { formatScheduledTime, hasMatchStarted, processDisplayData } from '@/src/utils/zoomMatchUtils';
import ZoomTeamScoreRow from '@/src/components/match/ZoomTeamScoreRow';

type MatchResultCardZoomProps = {
  game: Game;
  tournamentId: string | number;
  editable?: boolean;
  onScoreUpdate?: (updatedGame: Game) => void;
  matchIndex?: number;
};

/**
 * Zoomed match result card for live scoring and detailed display.
 * Supports real-time updates, point increment/undo, and super tie-break handling.
 */
export default function MatchResultCardZoom({
  game,
  tournamentId,
  editable = false,
  onScoreUpdate,
  matchIndex = 0,
}: MatchResultCardZoomProps) {
  const [currentGame, setCurrentGame] = useState<Game>(game);
  const [loading, setLoading] = useState<TeamSide | null>(null);
  const [undoLoading, setUndoLoading] = useState(false);
  const [matchFormat, setMatchFormat] = useState<import('@/src/types/matchFormat').MatchFormat | null>(null);

  const formattedScheduledTime = formatScheduledTime(currentGame.scheduledTime);
  const hasTournamentContext = Boolean(tournamentId);

  // --- Realtime & API ---
  useRealtimeGame({
    gameId: game.id,
    enabled: !currentGame.finished,
    onScoreUpdate: (dto) => {
      if (dto.score) {
        const newScore = processSuperTieBreakScore(dto.score, matchFormat);
        setCurrentGame((prev) => ({
          ...prev,
          score: newScore,
          winnerSide: dto.winner || prev.winnerSide,
          finished: dto.winner ? true : prev.finished,
        }));
      }
      if (onScoreUpdate) {
        onScoreUpdate(currentGame);
      }
    },
  });

  useEffect(() => {
    async function loadFormat() {
      const stage = game.round?.stage;
      if (stage && hasTournamentContext) {
        try {
          const format = await fetchMatchFormat(String(tournamentId), stage);
          setMatchFormat(format);
        } catch (e) {
          console.error('Erreur fetchMatchFormat:', e);
        }
      }
    }
    loadFormat();
  }, [game.round?.stage, hasTournamentContext, tournamentId, game.id]);

  const callIncrementEndpoint = (teamSide: TeamSide) => {
    return incrementGamePoint(tournamentId, game.id, teamSide);
  };

  const callUndoEndpoint = () => {
    return undoGamePoint(tournamentId, game.id);
  };

  const handlePointChange = async (teamSide: TeamSide) => {
    if (!editable) return;
    setLoading(teamSide);
    const prevScore = currentGame.score;
    try {
      const result = await callIncrementEndpoint(teamSide);
      if (result && result.score) {
        const newScore = processSuperTieBreakScore(result.score, matchFormat);
        setCurrentGame((prev) => ({ ...prev, score: newScore }));
      }
      if (result?.winner != null) {
        setCurrentGame((prev) => ({ ...prev, winnerSide: result.winner ? (result.winner as TeamSide) : prev.winnerSide }));
      }
    } catch (e) {
      setCurrentGame((prev) => ({ ...prev, score: prevScore }));
    } finally {
      setLoading(null);
    }
  };

  const handleUndo = async () => {
    if (!editable) return;
    setUndoLoading(true);
    try {
      const result = await callUndoEndpoint();
      if (result?.score) {
        const newScore = processSuperTieBreakScore(result.score, matchFormat);
        setCurrentGame((prev) => ({
          ...prev,
          score: newScore,
          finished: false,
          winnerSide: undefined
        }));
      }
      if (result?.winner != null) {
        setCurrentGame((prev) => ({ ...prev, winnerSide: result.winner ? (result.winner as TeamSide) : prev.winnerSide }));
      }
      if (onScoreUpdate) onScoreUpdate(currentGame);
    } catch (e) {
      console.error('Erreur undo:', e);
    } finally {
      setUndoLoading(false);
    }
  };

  // --- Préparation des Données d'Affichage ---
  const teams: (PlayerPair | null)[] = [currentGame.teamA ?? null, currentGame.teamB ?? null];

  const {
    setScoresA,
    setScoresB,
    tieBreakPointA,
    tieBreakPointB
  } = processDisplayData(currentGame, matchFormat);

  // Compute winner sides
  const winnerSideA = currentGame.winnerSide;
  const winnerSideB = currentGame.winnerSide;

  return (
    <div className="w-full max-w-2xl mx-auto font-sans">

      {/* Container Principal "Carte" */}
      <div className="bg-background rounded-2xl border shadow-sm overflow-hidden flex flex-col relative">

        {/* En-tête : Infos Match */}
        <div className="px-4 py-3 bg-muted/30 border-b flex items-center justify-between">
            <div className="flex items-center gap-4">
              {!currentGame.finished && hasMatchStarted(currentGame.score) && <LiveMatchIndicator />}
              {currentGame.finished && (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600">
                  <Trophy className="w-3 h-3" /> Terminé
                </span>
              )}
            </div>

            <div className="flex items-center gap-3 text-xs font-medium text-muted-foreground">
                {currentGame.court && (
                  <div className="flex items-center gap-1">
                    <MapPin className="w-3 h-3" />
                    <span>{currentGame.court}</span>
                  </div>
                )}
                {formattedScheduledTime && (
                  <div className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    <span>{formattedScheduledTime}</span>
                  </div>
                )}
            </div>
        </div>

        {/* Corps : Scoreboard */}
        {/* OPTIMISATION MOBILE: p-1 sur mobile pour maximiser la largeur */}
        <div className="p-1 sm:p-4 space-y-2">

        {/* Équipe A */}
            <ZoomTeamScoreRow
                team={teams[0]}
                teamIndex={0}
                gamePoint={currentGame.score?.currentGamePointA}
                setScores={setScoresA}
                tieBreakPoint={tieBreakPointA}
                teamSide="TEAM_A"
                editable={editable}
                loading={loading === 'TEAM_A'}
                onPointChange={handlePointChange}
                winnerSide={winnerSideA}
                isFinished={currentGame.finished}
            />

            {/* Équipe B */}
            <ZoomTeamScoreRow
                team={teams[1]}
                teamIndex={1}
                gamePoint={currentGame.score?.currentGamePointB}
                setScores={setScoresB}
                tieBreakPoint={tieBreakPointB}
                teamSide="TEAM_B"
                editable={editable}
                loading={loading === 'TEAM_B'}
                onPointChange={handlePointChange}
                winnerSide={winnerSideB}
                isFinished={currentGame.finished}
            />

        </div>

        {/* Pied de page : Actions Admin (Undo) */}
        {editable && (
            <div className="bg-muted/20 px-4 py-3 border-t flex justify-end items-center gap-4">
               {/* Le bouton Undo est traité comme le "Moins" ou "Retour arrière" */}
               <button
                  onClick={handleUndo}
                  disabled={undoLoading}
                  className="btn btn-outline btn-sm gap-2 border-border hover:bg-background hover:text-foreground"
                >
                  {undoLoading ? <Loader2 className="w-4 h-4 animate-spin"/> : <Undo2 className="w-4 h-4" />}
                  <span>Annuler le dernier point</span>
               </button>
            </div>
        )}
      </div>
    </div>
  );
}