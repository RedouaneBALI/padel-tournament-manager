'use client';

import React, { useState, useRef } from 'react';
import 'react-clock/dist/Clock.css';
import { setHours, setMinutes } from 'date-fns';
import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';
import { Edit3, Save, X } from 'lucide-react';
import { updateGameDetails } from '@/src/api/tournamentApi';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onInfoSaved?: (result: { tournamentUpdated: boolean; winner: string | null }) => void;
  onTimeChanged?: (gameId: string, newTime: string) => void; // Nouvelle prop
  winnerSide?: number;
  stage?: string;
  court?: string;
  scheduledTime?: string;
  pool?: { name?: string };
}

export default function MatchResultCard({
  teamA,
  teamB,
  editable = false,
  gameId,
  score,
  tournamentId,
  onInfoSaved,
  onTimeChanged, // Nouvelle prop
  winnerSide,
  stage,
  court,
  scheduledTime,
  pool
}: Props) {
  const [localCourt, setLocalCourt] = useState(court || 'Court central');
  const [localScheduledTime, setLocalScheduledTime] = useState(scheduledTime || '00:00');
  const [editing, setEditing] = useState(false);
  const [scores, setScores] = useState<string[][]>(() => {
    const initialScores: string[][] = [[], []];
    for (let i = 0; i < 3; i++) {
      initialScores[0][i] = score?.sets[i]?.teamAScore?.toString() || '';
      initialScores[1][i] = score?.sets[i]?.teamBScore?.toString() || '';
    }
    return initialScores;
  });

  const [initialScores, setInitialScores] = useState<string[][]>(() => {
    const initial: string[][] = [[], []];
    for (let i = 0; i < 3; i++) {
      initial[0][i] = score?.sets[i]?.teamAScore?.toString() || '';
      initial[1][i] = score?.sets[i]?.teamBScore?.toString() || '';
    }
    return initial;
  });

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
        nextTeamIndex = 1;
        nextSetIndex = setIndex;
      } else {
        nextTeamIndex = 0;
        nextSetIndex = setIndex + 1;
        if (nextSetIndex >= scores[0].length) {
          nextSetIndex = 0;
        }
      }

      const nextInput = inputRefs.current[nextTeamIndex][nextSetIndex];
      if (nextInput) {
        nextInput.focus();
      }
    }
  };

  const saveGameDetails = async () => {
    try {
      const scorePayload = convertToScoreObject(scores);
      const result = await updateGameDetails(tournamentId, gameId, scorePayload, localCourt, localScheduledTime);

      // Mise à jour locale de l'heure si elle a changé
      if (onTimeChanged && localScheduledTime !== scheduledTime) {
        onTimeChanged(gameId, localScheduledTime);
      }

      // Appel de l'ancien callback si présent
      if (onInfoSaved) {
        onInfoSaved(result);
      }
    } catch (error) {
      console.error('Erreur API:', error);
    }
  };

  return (
    <div
      className={`relative bg-card border border-border rounded-lg shadow-sm overflow-hidden w-full sm:max-w-[400px] transition-all duration-200 ${
        editing ? 'ring-2 ring-edit-border bg-edit-bg/30' : ''
      }`}
    >
      <div className="flex justify-between items-start px-2 pt-2">
        {(pool?.name || stage) && (
          <div className="inline-block bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-100 text-xs rounded mt-1 mx-1 px-3 py-0.5">
            {pool?.name ? `Groupe ${pool.name}` : stage}
          </div>
        )}
        {editable && (
          <div className="z-10">
            {editing ? (
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    setScores([...initialScores]);
                    setLocalCourt(court || 'Court central');
                    setLocalScheduledTime(scheduledTime || '00:00');
                    setEditing(false);
                  }}
                  className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-9 rounded-md px-3"
                >
                  <X className="h-3 w-3 mr-1" />
                  Annuler
                </button>
                <button
                  onClick={async () => {
                    await saveGameDetails();
                    setInitialScores([...scores]);
                    setEditing(false);
                  }}
                  className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 bg-green-600 text-textWhite hover:bg-green-700 h-9 rounded-md px-3 shadow-md"
                >
                  <Save className="h-3 w-3 mr-1" />
                  Sauver
                </button>
              </div>
            ) : (
              <button
                onClick={() => setEditing(true)}
                className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-7 w-7 p-0 hover:bg-primary/10 hover:text-primary"
                title="Modifier les scores"
              >
                <Edit3 className="h-4 w-4" />
              </button>
            )}
          </div>
        )}
      </div>

      <div className={`divide-y divide-border`}>
        <TeamScoreRow
          team={teamA}
          teamIndex={0}
          scores={scores[0]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [newScores, prev[1]])}
          inputRefs={{ current: inputRefs.current[0] }}
          handleKeyDown={handleKeyDown}
          winnerSide={winnerSide}
        />
        <TeamScoreRow
          team={teamB}
          teamIndex={1}
          scores={scores[1]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [prev[0], newScores])}
          inputRefs={{ current: inputRefs.current[1] }}
          handleKeyDown={handleKeyDown}
          winnerSide={winnerSide}
        />
      </div>
      <div
        className={`border-t border-border px-4 py-2 text-sm ${
          editing
            ? 'bg-white text-gray-900'
            : 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-100'
        }`}
      >
        {editing ? (
          <div className="flex gap-4 items-center">
            <input
              type="text"
              value={localCourt}
              onChange={(e) => setLocalCourt(e.target.value)}
              className="px-2 py-1 rounded border text-sm text-gray-900 bg-white"
              placeholder="Court"
            />
            <input
              type="time"
              step="300"
              value={localScheduledTime}
              onChange={(e) => setLocalScheduledTime(e.target.value)}
              className="px-2 py-1 rounded border text-sm text-gray-900 bg-white"
            />
          </div>
        ) : (
          <div className="flex justify-between">
            <span>{localCourt}</span>
            <span>{localScheduledTime}</span>
          </div>
        )}
      </div>
    </div>
  );
}