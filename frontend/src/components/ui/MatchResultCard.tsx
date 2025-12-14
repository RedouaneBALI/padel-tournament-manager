'use client';

import React, { useState, useRef } from 'react';
import 'react-clock/dist/Clock.css';
import { setHours, setMinutes } from 'date-fns';
import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';
import { Edit3 } from 'lucide-react';
import SaveAndCancelButtons from '@/src/components/ui/SaveAndCancelButtons';
import { updateGameDetails } from '@/src/api/tournamentApi';
import { normalizeGroup, groupBadgeClasses, formatGroupLabel } from '@/src/utils/groupBadge';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';
import { confirmAlert } from 'react-confirm-alert';
import { Stage } from '@/src/types/stage';
import { initializeScoresFromScore } from '@/src/utils/scoreUtils';
import { useGameSave } from '@/src/hooks/useGameSave';

function computeVisibleSets(setsToWin: number | undefined, scores: string[][]): number {
  const setsToWinValue = setsToWin ?? 2;
  if (setsToWinValue === 1) {
    return 1;
  } else if (setsToWinValue === 2) {
    // Compute wins for first two sets if both scores are numeric
    const firstSetA = parseInt(scores[0][0], 10);
    const firstSetB = parseInt(scores[1][0], 10);
    const secondSetA = parseInt(scores[0][1], 10);
    const secondSetB = parseInt(scores[1][1], 10);

    const firstSetValid = !isNaN(firstSetA) && !isNaN(firstSetB);
    const secondSetValid = !isNaN(secondSetA) && !isNaN(secondSetB);

    let winsA = 0;
    let winsB = 0;

    if (firstSetValid) {
      if (firstSetA > firstSetB) winsA++;
      else if (firstSetB > firstSetA) winsB++;
    }
    if (secondSetValid) {
      if (secondSetA > secondSetB) winsA++;
      else if (secondSetB > secondSetA) winsB++;
    }

    if (winsA === 1 && winsB === 1) {
      return 3;
    } else {
      return 2;
    }
  } else {
    return 3;
  }
}

function computeIsInProgress(finished: boolean, score: Score | undefined, scores: string[][]): boolean {
  const propHasScores = !!(score?.sets && score.sets.some(set => (set.teamAScore !== null && set.teamAScore !== undefined) || (set.teamBScore !== null && set.teamBScore !== undefined)));
  const localHasScores = !!(scores && (scores[0].some(s => s !== '' && s !== undefined && s !== null) || scores[1].some(s => s !== '' && s !== undefined && s !== null)));
  return !finished && (propHasScores || localHasScores);
}

function computeBadgeLabel(pool: { name?: string } | undefined, matchIndex: number | undefined, totalMatches: number | undefined): string {
  return pool?.name
    ? formatGroupLabel(pool.name)
    : (matchIndex !== undefined && totalMatches !== undefined)
      ? `${matchIndex + 1}/${totalMatches}`
      : '';
}

function computeShowChampion(stage: string | undefined, finished: boolean, winnerSide: number | undefined, teamIndex: number): boolean {
  try {
    const stageStr = String(stage || '').toLowerCase();
    const isFinalStage = stage === Stage.FINAL || stageStr === 'finale' || stageStr === 'final' || stageStr.includes('final');
    return finished && isFinalStage && winnerSide !== undefined && winnerSide === teamIndex;
  } catch (e) { return false; }
}

function useScoreSyncing(score: Score | undefined, editing: boolean, setScores: (scores: string[][]) => void, setInitialScores: (scores: string[][]) => void, setIsForfeit: (isForfeit: boolean) => void, setForfeitedBy: (forfeitedBy: 'TEAM_A' | 'TEAM_B' | null) => void) {
  const prevScoreSerializedRef = React.useRef<string | null>(null);

  React.useEffect(() => {
    const serialized = JSON.stringify(score || null);
    if (editing) {
      prevScoreSerializedRef.current = serialized;
      return;
    }
    if (prevScoreSerializedRef.current === serialized) return;
    prevScoreSerializedRef.current = serialized;
    const newScores = initializeScoresFromScore(score);
    setScores(newScores);
    setInitialScores(newScores.map(arr => [...arr]));
    setIsForfeit(score?.forfeit || false);
    setForfeitedBy(score?.forfeitedBy || null);
  }, [score, editing, setScores, setInitialScores, setIsForfeit, setForfeitedBy]);
}

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onInfoSaved?: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  onTimeChanged?: (gameId: string, newTime: string) => void;
  onGameUpdated?: (gameId: string, changes: { scheduledTime?: string; court?: string }) => void;
  winnerSide?: number;
  stage?: string;
  court?: string;
  scheduledTime?: string;
  pool?: { name?: string };
  setsToWin?: number;
  finished?: boolean;
  matchIndex?: number;
  totalMatches?: number;
  isFirstRound?: boolean;
  updateGameFn?: (gameId: string, scorePayload: Score, court: string, scheduledTime: string) => Promise<any>;
}

export default function MatchResultCard({
  teamA,
  teamB,
  editable = false,
  gameId,
  score,
  tournamentId,
  onInfoSaved,
  onTimeChanged,
  onGameUpdated,
  winnerSide,
  stage,
  court,
  scheduledTime,
  pool,
  setsToWin,
  finished = true,
  matchIndex,
  totalMatches,
  isFirstRound = false,
  updateGameFn,
}: Props) {
  const group = normalizeGroup(pool?.name);

  const [localCourt, setLocalCourt] = useState(court || 'Court central');
  const [localScheduledTime, setLocalScheduledTime] = useState(scheduledTime || '00:00');
  const [editing, setEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Keep local court/time in sync with props when not editing
  React.useEffect(() => {
    if (!editing) {
      setLocalCourt(court || 'Court central');
      setLocalScheduledTime(scheduledTime || '00:00');
    }
  }, [court, scheduledTime, editing]);

  const [isForfeit, setIsForfeit] = useState(score?.forfeit || false);
  const [forfeitedBy, setForfeitedBy] = useState<'TEAM_A' | 'TEAM_B' | null>(score?.forfeitedBy || null);

  const [scores, setScores] = useState<string[][]>(() => initializeScoresFromScore(score));
  const [initialScores, setInitialScores] = useState<string[][]>(() => initializeScoresFromScore(score));

  // Resync scores when `score` prop is updated from parent (e.g. polling)
  // Only resync when the serialized score actually changed to avoid clobbering local edits
  useScoreSyncing(score, editing, setScores, setInitialScores, setIsForfeit, setForfeitedBy);

  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array.from({ length: 2 }, () => Array(3).fill(null))
  );

  // Helper to compute visibleSets based on setsToWin and first two sets
  const visibleSets = computeVisibleSets(setsToWin, scores);

  const { saveGame, isSaving: hookIsSaving } = useGameSave(gameId, tournamentId, updateGameFn, onInfoSaved, onTimeChanged, onGameUpdated);

  const applyApiScore = (apiScore: Score) => {
    const appliedScores = initializeScoresFromScore(apiScore);
    setScores(appliedScores);
    setInitialScores(appliedScores.map(arr => [...arr]));
    setIsForfeit(apiScore.forfeit || false);
    setForfeitedBy(apiScore.forfeitedBy || null);
    try {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('game-updated', { detail: { gameId, score: apiScore } }));
      }
    } catch (e) { /* ignore */ }
  };

  const saveGameDetails = async () => {
    try {
      const scorePayload = convertToScoreObject(scores, visibleSets, isForfeit, forfeitedBy);

      // Utiliser updateGameFn si fourni (pour les matchs standalone), sinon updateGameDetails (pour les matchs de tournoi)
      const result = updateGameFn
        ? await updateGameFn(gameId, scorePayload, localCourt, localScheduledTime)
        : await updateGameDetails(tournamentId, gameId, scorePayload, localCourt, localScheduledTime);

      // Ensure local state reflects saved values immediately (prevents blank UI before parent updates)
      setLocalCourt(localCourt);
      setLocalScheduledTime(localScheduledTime);

      // If API returned the updated score, apply it to local state so UI reflects 'in progress' immediately
      if (result && result.score) {
        try {
          const apiScore = result.score as Score;
          applyApiScore(apiScore);
        } catch (e) {
          console.error('Apply API score error', e);
        }
      } else {
        try {
          const payloadScore = scorePayload as Score;
          applyApiScore(payloadScore);
        } catch (e) { /* ignore */ }
       }

      if (onTimeChanged && localScheduledTime !== scheduledTime) {
        onTimeChanged(gameId, localScheduledTime);
      }

      // Notify parent of any updated fields (court and/or scheduledTime)
      if (onGameUpdated) {
        const changes: { scheduledTime?: string; court?: string } = {};
        if (localScheduledTime !== scheduledTime) changes.scheduledTime = localScheduledTime;
        if (localCourt !== (court || '')) changes.court = localCourt;
        if (Object.keys(changes).length > 0) onGameUpdated(gameId, changes);
      }

      if (onInfoSaved) {
        onInfoSaved(result);
      }
    } catch (error) {
      console.error('Erreur API:', error);
    }
  };

  const handleSave = async () => {
    if (isSaving) return;

    const doSave = async () => {
      setIsSaving(true);
      try {
        await saveGameDetails();
        setInitialScores([...scores]);
        setEditing(false);
        // mark that the first-match confirmation has been shown (if applicable)
        try {
          if (isFirstRound && matchIndex === 0 && tournamentId && typeof window !== 'undefined') {
            const key = `ptm_first_match_confirmed_${tournamentId}`;
            try { sessionStorage.setItem(key, '1'); } catch (e) { /* ignore */ }
          }
        } catch (e) {
          // ignore storage errors
        }
      } finally {
        setIsSaving(false);
      }
    };

    // If this is the first match and the user hasn't previously confirmed for this tournament,
    // show a modal confirm like in the draw generation flow. If confirmed, save; otherwise do nothing.
    try {
      // Only show the 'start tournament' confirmation when editing the very first match
      // of the tournament (not the first match of any round). `isFirstRound` is passed
      // from the parent when the current round is the tournament's first round.
      if (isFirstRound && matchIndex === 0 && tournamentId && typeof window !== 'undefined') {
        const key = `ptm_first_match_confirmed_${tournamentId}`;
        if (!sessionStorage.getItem(key)) {
          confirmAlert({
            title: 'Confirmer le démarrage du tournoi',
            message:
              "En modifiant le score du premier match vous démarrez le tournoi. Cette action empêchera la modification du format du tournoi. Voulez-vous continuer ?",
            buttons: [
              {
                label: 'Oui',
                onClick: async () => {
                  await doSave();
                },
              },
              {
                label: 'Annuler',
                onClick: () => {
                  // nothing to do, user cancelled
                },
              },
            ],
          });
          return;
        }
      }
    } catch (e) {
      // If confirm fails for whatever reason, fall back to direct save
      console.error('Erreur confirm dialog:', e);
    }

    // Default: just save
    await doSave();
  };

  // Calculer si le match est en cours
  const isInProgress = computeIsInProgress(finished, score, scores);

  // Calculer le contenu du badge (garder la logique en dehors du JSX pour satisfaire TypeScript)
  const badgeLabel = computeBadgeLabel(pool, matchIndex, totalMatches);

  return (
    <div
      aria-busy={hookIsSaving}
      onClick={(e) => {
        if (editing) {
          e.stopPropagation();
        }
      }}
      className={`relative rounded-lg overflow-hidden w-full sm:max-w-[400px] transition-all duration-200 border border-gray-300
        ${editing
          ? 'shadow-2xl bg-edit-bg/30'
          : 'shadow-sm bg-card'
        }
        ${isInProgress ? 'ring-2 ring-red-500/20' : ''}
        `}
    >
      {/* Indicateur match en cours - en mode non-éditable */}
      {isInProgress && !editable && (
        <div className="absolute top-2 right-2 z-30">
          <LiveMatchIndicator showLabel={true} />
        </div>
      )}

      {hookIsSaving && (
        <div className="absolute inset-0 bg-background/40 backdrop-blur-[1px] z-20 flex items-center justify-center" aria-hidden>
          <CenteredLoader />
        </div>
      )}
      <div className="flex justify-between items-start px-2 pt-2">
        {(badgeLabel !== '') && (
          <div
            className={[
              'inline-block text-xs font-medium rounded mt-1 mx-1 px-3 py-0.5',
              pool?.name ? groupBadgeClasses(group) : 'bg-border text-foreground'
            ].join(' ')}
          >
            {badgeLabel}
          </div>
        )}
        {editable && (
          <div className="z-10 ml-auto flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
            {/* Point rouge en mode éditable si match en cours */}
            {isInProgress && <LiveMatchIndicator showLabel={false} />}

            {editing ? (
              <SaveAndCancelButtons
                isSaving={isSaving}
                onCancel={() => {
                  setScores([...initialScores]);
                  setLocalCourt(court || 'Court central');
                  setLocalScheduledTime(scheduledTime || '00:00');
                  setIsForfeit(score?.forfeit || false);
                  setForfeitedBy(score?.forfeitedBy || null);
                  setEditing(false);
                }}
                bindEnter={editing}
                onSave={handleSave}
              />
            ) : (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setEditing(true);
                }}
                className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-7 w-7 p-0 hover:bg-primary/10 hover:text-primary"
                title="Modifier les scores"
              >
                <Edit3 className="h-4 w-4" />
              </button>
            )}
          </div>
        )}
      </div>

      <div className={`divide-y divide-gray-200`}>
        {/* Determine if this is the final stage (robust to strings and enum) */}
        {/* showChampion will be true for the winning side if this is the final and the match is finished */}
        <TeamScoreRow
          team={teamA}
          teamIndex={0}
          scores={scores[0]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [newScores, prev[1]])}
          inputRefs={{ current: inputRefs.current[0] }}
          handleKeyDown={handleKeyDown}
          winnerSide={isForfeit ? (forfeitedBy === 'TEAM_B' ? 0 : undefined) : winnerSide}
          visibleSets={visibleSets}
          computeTabIndex={(tIdx, sIdx) => sIdx * 2 + (tIdx + 1)}
          showChampion={computeShowChampion(stage, finished, winnerSide, 0)}
          forfeited={isForfeit && forfeitedBy === 'TEAM_A'}
          showAbSlot={isForfeit}
          onToggleForfeit={() => {
            if (isForfeit && forfeitedBy === 'TEAM_A') {
              setIsForfeit(false);
              setForfeitedBy(null);
            } else {
              setIsForfeit(true);
              setForfeitedBy('TEAM_A');
            }
          }}
        />
        <TeamScoreRow
          team={teamB}
          teamIndex={1}
          scores={scores[1]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [prev[0], newScores])}
          inputRefs={{ current: inputRefs.current[1] }}
          handleKeyDown={handleKeyDown}
          winnerSide={isForfeit ? (forfeitedBy === 'TEAM_A' ? 1 : undefined) : winnerSide}
          visibleSets={visibleSets}
          computeTabIndex={(tIdx, sIdx) => sIdx * 2 + (tIdx + 1)}
          showChampion={computeShowChampion(stage, finished, winnerSide, 1)}
          forfeited={isForfeit && forfeitedBy === 'TEAM_B'}
          showAbSlot={isForfeit}
          onToggleForfeit={() => {
            if (isForfeit && forfeitedBy === 'TEAM_B') {
              setIsForfeit(false);
              setForfeitedBy(null);
            } else {
              setIsForfeit(true);
              setForfeitedBy('TEAM_B');
            }
          }}
        />
      </div>

      <div
        className={[
          'border-t border-gray-300 px-4 py-2 text-sm',
          pool?.name
            ? groupBadgeClasses(group)
            : (editing
                ? 'bg-card text-foreground'
                : 'bg-background text-foreground dark:bg-primary dark:text-on-primary')
        ].join(' ')}
      >
        {editing ? (
          <div className="flex gap-4 items-center">
            <input
              type="text"
              value={localCourt}
              onChange={(e) => setLocalCourt(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === 'NumpadEnter') {
                  e.preventDefault();
                  void handleSave();
                }
              }}
              enterKeyHint="done"
              className="px-2 py-1 rounded border text-sm text-foreground bg-card"
              placeholder="Court"
            />
            <input
              type="time"
              step="300"
              value={localScheduledTime}
              onChange={(e) => setLocalScheduledTime(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === 'NumpadEnter') {
                  e.preventDefault();
                  void handleSave();
                }
              }}
              enterKeyHint="done"
              className="px-2 py-1 rounded border text-sm text-foreground bg-card"
            />
          </div>
        ) : (
          <div className="flex justify-between">
            <span>{court ?? localCourt}</span>
            <span>{scheduledTime ?? localScheduledTime}</span>
          </div>
        )}
      </div>
    </div>
  );
}
