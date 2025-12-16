import React from 'react';
import { Score } from '@/src/types/score';
import { updateGameDetails } from '@/src/api/tournamentApi';
import { initializeScoresFromScore, convertToScoreObject } from '@/src/utils/scoreUtils';
import { confirmAlert } from 'react-confirm-alert';

interface UseSaveLogicParams {
  isSaving: boolean;
  setIsSaving: (saving: boolean) => void;
  scores: string[][];
  visibleSets: number;
  isForfeit: boolean;
  forfeitedBy: 'TEAM_A' | 'TEAM_B' | null;
  gameId: string;
  tournamentId: string;
  localCourt: string;
  localScheduledTime: string;
  updateGameFn: any;
  onInfoSaved: any;
  onTimeChanged: any;
  onGameUpdated: any;
  setScores: (scores: string[][]) => void;
  setInitialScores: (scores: string[][]) => void;
  setEditing: (editing: boolean) => void;
  setIsForfeit: (isForfeit: boolean) => void;
  setForfeitedBy: (forfeitedBy: 'TEAM_A' | 'TEAM_B' | null) => void;
  setLocalCourt: (court: string) => void;
  setLocalScheduledTime: (time: string) => void;
  court: string | undefined;
  scheduledTime: string | undefined;
  isFirstRound: boolean;
  matchIndex: number | undefined;
  setLocalWinnerSide: (winner: number | undefined) => void;
  setLocalFinished: (finished: boolean) => void;
  matchFormat: any;
}

export function useSaveLogic(params: UseSaveLogicParams) {
  const {
    isSaving,
    setIsSaving,
    scores,
    visibleSets,
    isForfeit,
    forfeitedBy,
    gameId,
    tournamentId,
    localCourt,
    localScheduledTime,
    updateGameFn,
    onInfoSaved,
    onTimeChanged,
    onGameUpdated,
    setScores,
    setInitialScores,
    setEditing,
    setIsForfeit,
    setForfeitedBy,
    setLocalCourt,
    setLocalScheduledTime,
    court,
    scheduledTime,
    isFirstRound,
    matchIndex,
    setLocalWinnerSide,
    setLocalFinished,
    matchFormat,
  } = params;

  const applyScoresToState = (score: Score) => {
    const appliedScores = initializeScoresFromScore(score);
    setScores(appliedScores);
    setInitialScores(appliedScores.map(arr => [...arr]));
    setIsForfeit(score.forfeit || false);
    setForfeitedBy(score.forfeitedBy || null);
  };

  const notifyGameUpdate = (gameId: string, score: Score) => {
    try {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('game-updated', { detail: { gameId, score } }));
      }
    } catch (e) { /* ignore */ }
  };

  const notifyCallbacks = (result: any) => {
    if (onTimeChanged && localScheduledTime !== scheduledTime) {
      onTimeChanged(gameId, localScheduledTime);
    }

    if (onGameUpdated) {
      const changes: { scheduledTime?: string; court?: string } = {};
      if (localScheduledTime !== scheduledTime) changes.scheduledTime = localScheduledTime;
      if (localCourt !== (court || '')) changes.court = localCourt;
      if (Object.keys(changes).length > 0) onGameUpdated(gameId, changes);
    }

    if (onInfoSaved) {
      onInfoSaved(result);
    }
  };

  const saveGameDetails = async () => {
    try {
      const scorePayload = convertToScoreObject(scores, visibleSets, isForfeit, forfeitedBy, matchFormat);
      const result = updateGameFn
        ? await updateGameFn(gameId, scorePayload, localCourt, localScheduledTime)
        : await updateGameDetails(tournamentId, gameId, scorePayload, localCourt, localScheduledTime);

      setLocalCourt(localCourt);
      setLocalScheduledTime(localScheduledTime);

      const scoreToApply = result?.score ?? scorePayload;
      try {
        applyScoresToState(scoreToApply);
        notifyGameUpdate(gameId, scoreToApply);
      } catch (e) {
        console.error('Apply score error', e);
      }

      notifyCallbacks(result);

      if (result?.winner) {
        let winnerSideValue: number | undefined;
        if (result.winner === 'TEAM_A') {
          winnerSideValue = 0;
        } else if (result.winner === 'TEAM_B') {
          winnerSideValue = 1;
        } else {
          winnerSideValue = undefined;
        }
        setLocalWinnerSide(winnerSideValue);
        setLocalFinished(true);
        try {
          window.dispatchEvent(new CustomEvent('game-updated', { detail: { gameId, score: result.score, winner: result.winner } }));
        } catch (e) { /* ignore */ }
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

    try {
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
                onClick: () => {
                  doSave();
                },
              },
              {
                label: 'Annuler',
                onClick: () => {},
              },
            ],
          });
          return;
        }
      }
    } catch (e) {
      console.error('Erreur confirm dialog:', e);
    }

    await doSave();
  };

  return { handleSave };
}
