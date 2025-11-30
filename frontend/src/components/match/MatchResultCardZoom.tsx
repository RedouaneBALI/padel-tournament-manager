import React, { useEffect, useState } from 'react';
import type { Game, PlayerPair, TeamSide } from '@/src/types/game';
import type { GamePoint } from '@/src/types/score';
import { cn } from '@/src/lib/utils';
import TeamRow from '@/src/components/ui/TeamRow';
import { incrementGamePoint, undoGamePoint, fetchMatchFormat } from '@/src/api/tournamentApi';
import { Undo2 } from 'lucide-react';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';

// Utilitaire pour formater les points
function formatPoints(points: number) {
  const pointMap = ['0', '15', '30', '40', 'A'];
  return pointMap[points] ?? points.toString();
}

// Nouvelle fonction pour formater les points tennis
function formatGamePoint(point: GamePoint | null | undefined) {
  if (point === null || point === undefined) return '0';
  switch (point) {
    case 'ZERO': return '0';
    case 'QUINZE': return '15';
    case 'TRENTE': return '30';
    case 'QUARANTE': return '40';
    case 'AVANTAGE': return 'A';
    default: return '-';
  }
}

// Extraction d'une ligne équipe + score + boutons
function TeamScoreRow({
  team,
  gamePoint,
  setScores,
  tieBreakPoint,
  teamSide,
  mode,
  loading,
  onPointChange,
  winnerSide,
}: {
  team: PlayerPair | null;
  gamePoint: GamePoint | null | undefined;
  setScores: (number | null)[];
  tieBreakPoint: number | null | undefined;
  teamSide: TeamSide;
  mode: 'spectator' | 'admin';
  loading: boolean;
  onPointChange: (teamSide: TeamSide) => void;
  winnerSide?: number;
}) {
  return (
    <div className="flex flex-1 items-center gap-2 min-h-[40px]">
      <div className="flex-1">
        <TeamRow team={team} teamIndex={teamSide === 'TEAM_A' ? 0 : 1} winnerSide={winnerSide} />
      </div>
      {/* Affichage des jeux (sets) */}
      <div className="flex gap-2 min-w-[60px] justify-center">
        {setScores.map((score, i) => (
          <span
            key={i}
            className="text-base font-bold text-foreground"
            style={{ letterSpacing: '0.04em' }}
          >
            {score ?? '-'}
          </span>
        ))}
      </div>
      {/* Affichage des points tennis ou tie-break */}
      <div className="w-10 flex justify-center">
        {tieBreakPoint !== null && tieBreakPoint !== undefined ? (
          <span className="text-lg font-bold text-blue-600" style={{ fontWeight: 700 }}>
            {tieBreakPoint}
          </span>
        ) : (
          <span className="text-lg font-medium text-muted-foreground" style={{ fontWeight: 500 }}>
            {formatGamePoint(gamePoint)}
          </span>
        )}
      </div>
      {mode === 'admin' && (
        <div className="flex flex-col gap-1 items-end min-w-[32px] ml-2">
          <button
            className="btn btn-outline btn-xs w-8"
            disabled={loading || (typeof winnerSide !== 'undefined')}
            onClick={() => onPointChange(teamSide)}
            type="button"
          >
            +
          </button>
        </div>
      )}
    </div>
  );
}

type MatchResultCardZoomProps = {
  game: Game;
  tournamentId: string | number;
  mode: 'spectator' | 'admin';
  onScoreUpdate?: (updatedGame: Game) => void;
};

export default function MatchResultCardZoom({
  game,
  tournamentId,
  mode,
  onScoreUpdate,
}: MatchResultCardZoomProps) {
  const [currentGame, setCurrentGame] = useState<Game>(game);
  const [loading, setLoading] = useState<TeamSide | null>(null);
  const [undoLoading, setUndoLoading] = useState(false);
  const [matchFormat, setMatchFormat] = useState<import('@/src/types/matchFormat').MatchFormat | null>(null);

  useEffect(() => {
    async function loadFormat() {
      const stage = game.round?.stage;
      if (stage && tournamentId) {
        try {
          const format = await fetchMatchFormat(String(tournamentId), stage);
          setMatchFormat(format);
        } catch (e) {
          console.error('Erreur fetchMatchFormat:', e);
        }
      }
    }
    loadFormat();
  }, [game.round?.stage, tournamentId]);

  const handlePointChange = async (teamSide: TeamSide) => {
    setLoading(teamSide);
    const prevScore = currentGame.score;
    try {
      const result = await incrementGamePoint(
        tournamentId,
        game.id,
        teamSide
      );
      if (result && result.score) {
        setCurrentGame((prev) => ({
          ...prev,
          score: result.score,
        }));
      }
      if (result && typeof result.winner !== 'undefined') {
        setCurrentGame((prev) => ({
          ...prev,
          winnerSide: result.winner,
        }));
      }
    } catch (e) {
      setCurrentGame((prev) => ({
        ...prev,
        score: prevScore,
      }));
    } finally {
      setLoading(null);
    }
  };

  // Ajout Undo global
  const handleUndo = async () => {
    setUndoLoading(true);
    try {
      const result = await undoGamePoint(
        tournamentId,
        game.id
      );
      if (result && result.score) {
        setCurrentGame((prev) => ({
          ...prev,
          score: result.score,
        }));
      }
      if (result && typeof result.winner !== 'undefined') {
        setCurrentGame((prev) => ({
          ...prev,
          winnerSide: result.winner,
        }));
      }
      if (onScoreUpdate) onScoreUpdate(currentGame);
    } catch (e) {
      // Optionnel : toast d'erreur
    } finally {
      setUndoLoading(false);
    }
  };

  // Récupère les infos d'équipes et scores
  const teams: (PlayerPair | null)[] = [currentGame.teamA ?? null, currentGame.teamB ?? null];
  // Affichage des jeux (sets)
  const gamesScore = currentGame.score?.sets?.map((set) => [set.teamAScore, set.teamBScore]) ?? [];
  const points = currentGame.score?.points ?? [0, 0];
  // Préparation des scores par set pour chaque équipe
  let setScoresA = currentGame.score?.sets?.map((set) => set.teamAScore) ?? [];
  let setScoresB = currentGame.score?.sets?.map((set) => set.teamBScore) ?? [];
  let tieBreakPointA = currentGame.score?.tieBreakPointA;
  let tieBreakPointB = currentGame.score?.tieBreakPointB;
  // Gestion du super tie-break en 3e set (affiche le score réel du set)
  const isSuperTieBreak = (
    (matchFormat?.superTieBreakInFinalSet || (currentGame.score?.tieBreakPointA != null || currentGame.score?.tieBreakPointB != null))
    && currentGame.score?.sets?.length === 3
  );
  if (isSuperTieBreak && currentGame.score?.sets?.[2]) {
    const set3 = currentGame.score.sets[2];
    setScoresA = [setScoresA[0], setScoresA[1], set3.tieBreakTeamA ?? setScoresA[2]];
    setScoresB = [setScoresB[0], setScoresB[1], set3.tieBreakTeamB ?? setScoresB[2]];
    // Si les tieBreakPointA/B sont null, on prend la valeur du tieBreakTeamA/B pour l'affichage du point en cours
    if (tieBreakPointA == null && set3.tieBreakTeamA != null) {
      tieBreakPointA = set3.tieBreakTeamA;
    }
    if (tieBreakPointB == null && set3.tieBreakTeamB != null) {
      tieBreakPointB = set3.tieBreakTeamB;
    }
  } else if (isSuperTieBreak) {
    setScoresA = [setScoresA[0], setScoresA[1], setScoresA[2]];
    setScoresB = [setScoresB[0], setScoresB[1], setScoresB[2]];
    if (tieBreakPointA == null && setScoresA[2] != null) {
      tieBreakPointA = setScoresA[2];
    }
    if (tieBreakPointB == null && setScoresB[2] != null) {
      tieBreakPointB = setScoresB[2];
    }
  } else {
    setScoresA = setScoresA.slice(0, 3);
    setScoresB = setScoresB.slice(0, 3);
  }

  return (
    <div className={cn('rounded-lg border p-4 bg-background flex flex-col gap-4')}>
      {/* Header: Live en haut à droite, non flottant */}
      <div className="flex flex-row items-center justify-between w-full min-h-[32px]">
        <div />
        {!currentGame.finished && (
          <LiveMatchIndicator showLabel={true} />
        )}
      </div>
      {/* Score et équipes + Undo */}
      <div className="flex justify-between items-center">
        <div className="flex-1 flex flex-col gap-2">
          <TeamScoreRow
            team={teams[0]}
            gamePoint={currentGame.score?.currentGamePointA}
            setScores={setScoresA}
            tieBreakPoint={currentGame.score?.tieBreakPointA}
            teamSide="TEAM_A"
            mode={mode}
            loading={loading === 'TEAM_A'}
            onPointChange={handlePointChange}
            winnerSide={currentGame.winnerSide === 'TEAM_A' ? 0 : currentGame.winnerSide === 'TEAM_B' ? 1 : undefined}
          />
          <TeamScoreRow
            team={teams[1]}
            gamePoint={currentGame.score?.currentGamePointB}
            setScores={setScoresB}
            tieBreakPoint={currentGame.score?.tieBreakPointB}
            teamSide="TEAM_B"
            mode={mode}
            loading={loading === 'TEAM_B'}
            onPointChange={handlePointChange}
            winnerSide={currentGame.winnerSide === 'TEAM_A' ? 0 : currentGame.winnerSide === 'TEAM_B' ? 1 : undefined}
          />
        </div>
        {mode === 'admin' && (
          <button
            className="btn btn-outline btn-xs ml-4 flex items-center justify-center"
            disabled={undoLoading}
            onClick={handleUndo}
            type="button"
            aria-label="Undo last point"
            title="Annuler le dernier point"
          >
            <Undo2 className="w-5 h-5" />
          </button>
        )}
      </div>
      {/* Détails supplémentaires (court, heure, etc.) si besoin */}
      <div className="flex justify-between text-xs text-muted-foreground">
        <span>
          {currentGame.court?.name && <>Court : {currentGame.court.name}</>}
        </span>
        <span>
          {currentGame.startTime && <>Début : {new Date(currentGame.startTime).toLocaleTimeString()}</>}
        </span>
        <span>
          {currentGame.number && <>Match n°{currentGame.number}</>}
        </span>
      </div>
    </div>
  );
}