'use client';

import React, { useState, useRef, useEffect } from 'react';
import { PlayerPair } from '@/types/playerPair';
import { Score } from '@/types/score';
import TeamScoreRow from '@/src/components/match/TeamScoreRow';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
}

export default function MatchResultCardLight({ teamA, teamB, editable = false, gameId, score, tournamentId }: Props) {
  const [editing, setEditing] = useState(false);
  const [scores, setScores] = useState<string[][]>(() => {
    const initialScores: string[][] = [[], []];
    for (let i = 0; i < 3; i++) {
      initialScores[0][i] = score?.sets[i]?.teamAScore?.toString() || '';
      initialScores[1][i] = score?.sets[i]?.teamBScore?.toString() || '';
    }
    return initialScores;
  });

  // Refs : un tableau par équipe, contenant refs inputs sets
  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array.from({ length: 2 }, () => Array(3).fill(null))
  );

  function convertToScoreObject(scores: string[][]) {
    const sets = scores[0].map((_, i) => {
      const teamAStr = scores[0][i];
      const teamBStr = scores[1][i];

      if ((teamAStr === '' || teamAStr === undefined) && (teamBStr === '' || teamBStr === undefined)) {
        return null;
      }

      return {
        teamAScore: teamAStr === '' ? null : parseInt(teamAStr, 10),
        teamBScore: teamBStr === '' ? null : parseInt(teamBStr, 10),
      };
    }).filter(set => set !== null);

    return { sets };
  }

  const handleKeyDown = (e: React.KeyboardEvent, teamIndex: number, setIndex: number) => {
    if (e.key === 'Tab') {
      e.preventDefault();

      let nextTeamIndex: number;
      let nextSetIndex: number;

      if (teamIndex === 0) {
        // Passage à la même colonne équipe 1
        nextTeamIndex = 1;
        nextSetIndex = setIndex;
      } else {
        // équipe 1 → équipe 0, set suivant
        nextTeamIndex = 0;
        nextSetIndex = setIndex + 1;
        if (nextSetIndex >= scores[0].length) {
          // boucle à début
          nextSetIndex = 0;
        }
      }

      const nextInput = inputRefs.current[nextTeamIndex][nextSetIndex];
      if (nextInput) {
        nextInput.focus();
      }
    }
  };

  const saveScores = async () => {
    try {
      const scorePayload = convertToScoreObject(scores);
      const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/games/${gameId}/score`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(scorePayload),
      });

      if (!response.ok) {
        throw new Error('Erreur lors de la sauvegarde');
      }

      // Optionnel : confirmation visuelle, toast, etc.
      console.log('Scores enregistrés avec succès');
    } catch (error) {
      console.error('Erreur API:', error);
      // Optionnel : affichage d'une erreur utilisateur
    }
  };

  return (
    <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px]">
      <div className="divide-y divide-gray-200 dark:divide-gray-700">
        <TeamScoreRow
          team={teamA}
          teamIndex={0}
          scores={scores[0]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [newScores, prev[1]])}
          inputRefs={{ current: inputRefs.current[0] }}
          handleKeyDown={handleKeyDown}
        />
        <TeamScoreRow
          team={teamB}
          teamIndex={1}
          scores={scores[1]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [prev[0], newScores])}
          inputRefs={{ current: inputRefs.current[1] }}
          handleKeyDown={handleKeyDown}
        />
      </div>

      {editable && (
        <div className="flex justify-end space-x-2 p-2 bg-gray-50 dark:bg-gray-900">
          {editing ? (
            <>
              <button
                onClick={() => setEditing(false)}
                className="text-sm px-3 py-1 text-gray-600 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-700 rounded transition-colors"
              >
                Annuler
              </button>
              <button
                onClick={async () => {
                  await saveScores();
                  setEditing(false);
                }}
                className="text-sm px-3 py-1 text-white bg-blue-600 hover:bg-blue-700 rounded transition-colors"
              >
                Enregistrer
              </button>
            </>
          ) : (
            <button
              onClick={() => setEditing(true)}
              className="text-sm px-3 py-1 text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors"
            >
              Modifier
            </button>
          )}
        </div>
      )}
    </div>
  );
}