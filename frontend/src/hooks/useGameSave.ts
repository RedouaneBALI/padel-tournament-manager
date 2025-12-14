import { useState } from 'react';
import { Score } from '@/src/types/score';
import { updateGameDetails } from '@/src/api/tournamentApi';

/**
 * Custom hook to handle game save logic.
 * @param gameId - The ID of the game to save.
 * @param tournamentId - The ID of the tournament.
 * @param updateGameFn - Optional custom update function for standalone games.
 * @param onInfoSaved - Callback when save is successful.
 * @param onTimeChanged - Callback when scheduled time changes.
 * @param onGameUpdated - Callback when game details are updated.
 * @returns Object with saveGame function and isSaving state.
 */
export function useGameSave(
  gameId: string,
  tournamentId: string,
  updateGameFn?: (gameId: string, scorePayload: Score, court: string, scheduledTime: string) => Promise<any>,
  onInfoSaved?: (result: { tournamentUpdated: boolean; winner: string | null }) => void,
  onTimeChanged?: (gameId: string, newTime: string) => void,
  onGameUpdated?: (gameId: string, changes: { scheduledTime?: string; court?: string }) => void
) {
  const [isSaving, setIsSaving] = useState(false);

  /**
   * Saves the game with the provided parameters.
   * @param scores - The current scores array.
   * @param isForfeit - Whether the game is forfeited.
   * @param forfeitedBy - Which team forfeited.
   * @param localCourt - The local court value.
   * @param localScheduledTime - The local scheduled time value.
   * @param court - The original court value.
   * @param scheduledTime - The original scheduled time value.
   * @param convertToScoreObject - Function to convert scores to Score object.
   * @param applyApiScore - Optional function to apply API score to local state.
   */
  const saveGame = async (
    scores: string[][],
    isForfeit: boolean,
    forfeitedBy: 'TEAM_A' | 'TEAM_B' | null,
    localCourt: string,
    localScheduledTime: string,
    court: string,
    scheduledTime: string,
    convertToScoreObject: (scores: string[][], isForfeit: boolean, forfeitedBy: 'TEAM_A' | 'TEAM_B' | null) => Score,
    applyApiScore?: (apiScore: Score) => void
  ) => {
    if (isSaving) return;

    setIsSaving(true);
    try {
      const scorePayload = convertToScoreObject(scores, isForfeit, forfeitedBy);

      // Utiliser updateGameFn si fourni (pour les matchs standalone), sinon updateGameDetails (pour les matchs de tournoi)
      const result = updateGameFn
        ? await updateGameFn(gameId, scorePayload, localCourt, localScheduledTime)
        : await updateGameDetails(tournamentId, gameId, scorePayload, localCourt, localScheduledTime);

      // Ensure local state reflects saved values immediately (prevents blank UI before parent updates)
      if (result && result.score && applyApiScore) {
        applyApiScore(result.score);
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
      throw error; // Re-throw to handle in component
    } finally {
      setIsSaving(false);
    }
  };

  return { saveGame, isSaving };
}
