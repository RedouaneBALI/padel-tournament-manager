import React, { useEffect, useState } from 'react';
import type { Game, PlayerPair, TeamSide } from '@/src/types/game';
import type { GamePoint } from '@/src/types/score';
import { cn } from '@/src/lib/utils';
import TeamRow from '@/src/components/ui/TeamRow';
import { incrementGamePoint, undoGamePoint, fetchMatchFormat, incrementStandaloneGamePoint, undoStandaloneGamePoint } from '@/src/api/tournamentApi';
import { Undo2, MapPin, Clock, Trophy, Plus, Loader2 } from 'lucide-react';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';
import { useRealtimeGame } from '@/src/hooks/useRealtimeGame';

// --- Utilitaires ---

function formatGamePoint(point: GamePoint | null | undefined) {
  if (point === null || point === undefined) return '0';
  switch (point) {
    case 'ZERO': return '0';
    case 'QUINZE': return '15';
    case 'TRENTE': return '30';
    case 'QUARANTE': return '40';
    case 'AVANTAGE': return 'AD';
    default: return '-';
  }
}

function formatScheduledTime(value?: string | null) {
  if (!value) return null;
  const trimmed = value.trim();
  if (/^\d{1,2}:\d{2}$/.test(trimmed)) return trimmed;
  const parsed = new Date(trimmed);
  if (Number.isNaN(parsed.getTime())) return trimmed;
  return parsed.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// --- Sous-composant pour une ligne d'équipe ---

function ModernTeamScoreRow({
  team,
  teamIndex,
  gamePoint,
  setScores,
  tieBreakPoint,
  teamSide,
  editable,
  loading,
  onPointChange,
  winnerSide,
}: {
  team: PlayerPair | null;
  teamIndex: number;
  gamePoint: GamePoint | null | undefined;
  setScores: (number | null)[];
  tieBreakPoint: number | null | undefined;
  teamSide: TeamSide;
  editable: boolean;
  loading: boolean;
  onPointChange: (teamSide: TeamSide) => void;
  winnerSide?: number;
}) {
  const isTeamA = teamSide === 'TEAM_A';
  const themeColor = isTeamA ? 'blue' : 'rose';
  const isWinner = winnerSide === teamIndex;

  // Style dynamique pour le point en cours
  const isTieBreakActive = tieBreakPoint !== null && tieBreakPoint !== undefined;
  const displayPoint = isTieBreakActive ? tieBreakPoint : formatGamePoint(gamePoint);

  return (
    <div className={cn(
      "relative flex items-center p-3 sm:p-4 rounded-xl transition-all duration-300",
      isWinner
        ? "bg-gradient-to-r from-yellow-500/10 to-transparent border border-yellow-500/20"
        : "bg-card border border-transparent shadow-sm"
    )}>

      {/* 1. Zone Nom d'équipe */}
      <div className="flex-1 min-w-0 mr-2 sm:mr-4">
        <TeamRow
          team={team}
          teamIndex={teamIndex}
          winnerSide={winnerSide}
          fontSize="text-base sm:text-lg"
          showChampion={isWinner}
          themeColor={themeColor}
        />
      </div>

      {/* 2. Zone Scores (Sets + Points) */}
      <div className="flex items-center gap-2 sm:gap-4">

        {/* Scores des Sets (Historique) */}
        <div className="flex gap-1 sm:gap-2">
          {setScores.map((score, i) => (
            <div
              key={i}
              className={cn(
                "w-7 h-8 sm:w-9 sm:h-10 flex items-center justify-center rounded text-sm sm:text-lg font-bold tabular-nums",
                score !== null ? "text-foreground bg-muted/50" : "opacity-0"
              )}
            >
              {score ?? '-'}
            </div>
          ))}
        </div>

        {/* Score du Jeu actuel (Point Tennis / Tie Break) */}
        <div
          className={cn(
            "w-10 h-10 sm:w-14 sm:h-12 flex items-center justify-center rounded-lg font-bold text-lg sm:text-2xl tabular-nums shadow-inner transition-colors",
            isTeamA
              ? "bg-blue-50 text-blue-700 dark:bg-blue-950/40 dark:text-blue-400 border border-blue-100 dark:border-blue-900"
              : "bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-400 border border-rose-100 dark:border-rose-900"
          )}
        >
             <span className={cn(isTieBreakActive ? "text-xl sm:text-2xl" : "")}>
               {displayPoint}
             </span>
        </div>

        {/* 3. Bouton d'action (+) */}
        {editable && (
          <button
            type="button"
            disabled={loading || (typeof winnerSide !== 'undefined')}
            onClick={() => onPointChange(teamSide)}
            className={cn(
              "w-10 h-10 sm:w-12 sm:h-12 flex items-center justify-center rounded-full shadow-sm transition-all active:scale-95 border",
              isTeamA
                ? "bg-blue-600 text-white hover:bg-blue-700 border-blue-700 shadow-blue-200 dark:shadow-none"
                : "bg-rose-600 text-white hover:bg-rose-700 border-rose-700 shadow-rose-200 dark:shadow-none",
              loading && "opacity-70 cursor-not-allowed"
            )}
            aria-label={`Ajouter point à ${isTeamA ? 'Équipe A' : 'Équipe B'}`}
          >
            {loading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Plus className="w-6 h-6 sm:w-7 sm:h-7" strokeWidth={3} />
            )}
          </button>
        )}
      </div>
    </div>
  );
}

// --- Composant Principal ---

type MatchResultCardZoomProps = {
  game: Game;
  tournamentId: string | number;
  editable?: boolean;
  onScoreUpdate?: (updatedGame: Game) => void;
  matchIndex?: number;
};

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
        let newScore = { ...dto.score };
        const isSuperTB = (
          (matchFormat?.superTieBreakInFinalSet || (newScore.tieBreakPointA != null || newScore.tieBreakPointB != null) || (newScore.sets?.[2]?.tieBreakTeamA != null || newScore.sets?.[2]?.tieBreakTeamB != null))
          && newScore.sets?.length === 3
        );
        if (isSuperTB && newScore.sets && newScore.sets.length === 3) {
          const set3 = newScore.sets[2];
          if (typeof set3.tieBreakTeamA === 'number') {
            newScore.sets[2].teamAScore = set3.tieBreakTeamA;
            newScore.tieBreakPointA = set3.tieBreakTeamA;
          }
          if (typeof set3.tieBreakTeamB === 'number') {
            newScore.sets[2].teamBScore = set3.tieBreakTeamB;
            newScore.tieBreakPointB = set3.tieBreakTeamB;
          }
        }
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
    return hasTournamentContext
      ? incrementGamePoint(tournamentId, game.id, teamSide)
      : incrementStandaloneGamePoint(game.id, teamSide);
  };

  const callUndoEndpoint = () => {
    return hasTournamentContext
      ? undoGamePoint(tournamentId, game.id)
      : undoStandaloneGamePoint(game.id);
  };

  const handlePointChange = async (teamSide: TeamSide) => {
    if (!editable) return;
    setLoading(teamSide);
    const prevScore = currentGame.score;
    try {
      const result = await callIncrementEndpoint(teamSide);
      if (result && result.score) {
        let newScore = { ...result.score };
        const isSuperTB = (
          (matchFormat?.superTieBreakInFinalSet || (newScore.tieBreakPointA != null || newScore.tieBreakPointB != null) || (newScore.sets?.[2]?.tieBreakTeamA != null || newScore.sets?.[2]?.tieBreakTeamB != null))
          && newScore.sets?.length === 3
        );
        if (isSuperTB && newScore.sets && newScore.sets.length === 3) {
          const set3 = newScore.sets[2];
          if (typeof set3.tieBreakTeamA === 'number') {
            newScore.sets[2].teamAScore = set3.tieBreakTeamA;
            newScore.tieBreakPointA = set3.tieBreakTeamA;
          }
          if (typeof set3.tieBreakTeamB === 'number') {
            newScore.sets[2].teamBScore = set3.tieBreakTeamB;
            newScore.tieBreakPointB = set3.tieBreakTeamB;
          }
        }
        setCurrentGame((prev) => ({ ...prev, score: newScore }));
      }
      if (result && typeof result.winner !== 'undefined') {
        setCurrentGame((prev) => ({ ...prev, winnerSide: result.winner }));
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
      if (result && result.score) {
        let newScore = { ...result.score };
        const isSuperTB = (
          (matchFormat?.superTieBreakInFinalSet || (newScore.tieBreakPointA != null || newScore.tieBreakPointB != null) || (newScore.sets?.[2]?.tieBreakTeamA != null || newScore.sets?.[2]?.tieBreakTeamB != null))
          && newScore.sets?.length === 3
        );
        if (isSuperTB && newScore.sets && newScore.sets.length === 3) {
          const set3 = newScore.sets[2];
          if (typeof set3.tieBreakTeamA === 'number') {
            newScore.sets[2].teamAScore = set3.tieBreakTeamA;
            newScore.tieBreakPointA = set3.tieBreakTeamA;
          }
          if (typeof set3.tieBreakTeamB === 'number') {
            newScore.sets[2].teamBScore = set3.tieBreakTeamB;
            newScore.tieBreakPointB = set3.tieBreakTeamB;
          }
        }
        setCurrentGame((prev) => ({ ...prev, score: newScore }));
      }
      if (result && typeof result.winner !== 'undefined') {
        setCurrentGame((prev) => ({ ...prev, winnerSide: result.winner }));
      }
      if (onScoreUpdate) onScoreUpdate(currentGame);
    } catch (e) {
      // toast error
    } finally {
      setUndoLoading(false);
    }
  };

  // --- Préparation des Données d'Affichage ---
  const teams: (PlayerPair | null)[] = [currentGame.teamA ?? null, currentGame.teamB ?? null];

  let setScoresA = currentGame.score?.sets?.map((set) => set.teamAScore) ?? [];
  let setScoresB = currentGame.score?.sets?.map((set) => set.teamBScore) ?? [];
  let tieBreakPointA = currentGame.score?.tieBreakPointA;
  let tieBreakPointB = currentGame.score?.tieBreakPointB;

  const isSuperTieBreak = (
    (matchFormat?.superTieBreakInFinalSet || (currentGame.score?.tieBreakPointA != null || currentGame.score?.tieBreakPointB != null) || (currentGame.score?.sets?.[2]?.tieBreakTeamA != null || currentGame.score?.sets?.[2]?.tieBreakTeamB != null))
    && currentGame.score?.sets?.length === 3
  );

  const MAX_SETS = 3;
  if (isSuperTieBreak && currentGame.score?.sets?.[2]) {
    const set3 = currentGame.score.sets[2];
    setScoresA = [setScoresA[0], setScoresA[1], set3.tieBreakTeamA ?? setScoresA[2]];
    setScoresB = [setScoresB[0], setScoresB[1], set3.tieBreakTeamB ?? setScoresB[2]];

    if ((tieBreakPointA == null || typeof tieBreakPointA === 'undefined') && set3.tieBreakTeamA != null) {
      tieBreakPointA = set3.tieBreakTeamA;
    }
    if ((tieBreakPointB == null || typeof tieBreakPointB === 'undefined') && set3.tieBreakTeamB != null) {
      tieBreakPointB = set3.tieBreakTeamB;
    }
  } else if (isSuperTieBreak) {
    setScoresA = [setScoresA[0], setScoresA[1], setScoresA[2]];
    setScoresB = [setScoresB[0], setScoresB[1], setScoresB[2]];
  } else {
    setScoresA = setScoresA.slice(0, 3);
    setScoresB = setScoresB.slice(0, 3);
  }

  while (setScoresA.length < MAX_SETS) { setScoresA.push(null); }
  while (setScoresB.length < MAX_SETS) { setScoresB.push(null); }

  return (
    <div className="w-full max-w-2xl mx-auto font-sans">

      {/* Container Principal "Carte" */}
      <div className="bg-background rounded-2xl border shadow-sm overflow-hidden flex flex-col relative">

        {/* En-tête : Infos Match */}
        <div className="px-4 py-3 bg-muted/30 border-b flex items-center justify-between">
            <div className="flex items-center gap-4">
              {!currentGame.finished && <LiveMatchIndicator />}
              {currentGame.finished && (
                <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
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
        <div className="p-2 sm:p-4 space-y-2">

            {/* Équipe A */}
            <ModernTeamScoreRow
                team={teams[0]}
                teamIndex={0}
                gamePoint={currentGame.score?.currentGamePointA}
                setScores={setScoresA}
                tieBreakPoint={tieBreakPointA}
                teamSide="TEAM_A"
                editable={editable}
                loading={loading === 'TEAM_A'}
                onPointChange={handlePointChange}
                winnerSide={currentGame.winnerSide === 'TEAM_A' ? 0 : currentGame.winnerSide === 'TEAM_B' ? 1 : undefined}
            />

            {/* Équipe B */}
            <ModernTeamScoreRow
                team={teams[1]}
                teamIndex={1}
                gamePoint={currentGame.score?.currentGamePointB}
                setScores={setScoresB}
                tieBreakPoint={tieBreakPointB}
                teamSide="TEAM_B"
                editable={editable}
                loading={loading === 'TEAM_B'}
                onPointChange={handlePointChange}
                winnerSide={currentGame.winnerSide === 'TEAM_A' ? 0 : currentGame.winnerSide === 'TEAM_B' ? 1 : undefined}
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