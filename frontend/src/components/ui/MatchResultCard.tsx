'use client';

import React, { useState, useRef } from 'react';
import { PlayerPair } from '@/types/playerPair';
import { Score } from '@/types/score';
import TeamScoreRow from '@/src/components/match/TeamScoreRow';
import { Edit3, Save, X } from 'lucide-react';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onScoreSaved: (result: { tournamentUpdated: boolean; winner: String | null }) => void;
  winnerSide?: number;
  stage?: string;
}

export default function MatchResultCard({ teamA, teamB, editable = false, gameId, score, tournamentId, onScoreSaved, winnerSide, stage }: Props) {
  const [editing, setEditing] = useState(false);
  const [scores, setScores] = useState<string[][]>(() => {
    const initialScores: string[][] = [[], []];
    for (let i = 0; i < 3; i++) {
      initialScores[0][i] = score?.sets[i]?.teamAScore?.toString() || '';
      initialScores[1][i] = score?.sets[i]?.teamBScore?.toString() || '';
    }
    return initialScores;
  });

  // Sauvegarder les scores initiaux pour pouvoir les restaurer lors de l'annulation
  const [initialScores, setInitialScores] = useState<string[][]>(() => {
    const initial: string[][] = [[], []];
    for (let i = 0; i < 3; i++) {
      initial[0][i] = score?.sets[i]?.teamAScore?.toString() || '';
      initial[1][i] = score?.sets[i]?.teamBScore?.toString() || '';
    }
    return initial;
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
    console.log("saveScores");
    try {
      const scorePayload = convertToScoreObject(scores);
      const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/games/${gameId}/score`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(scorePayload),
      });

      if (!response.ok) throw new Error('Erreur lors de la sauvegarde');

      const result = await response.json();
      onScoreSaved(result);
    } catch (error) {
      console.error('Erreur API:', error);
    }
  };

  return (
    <div className={`relative bg-card border border-border rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px] transition-all duration-200 ${
      editing ? 'ring-2 ring-edit-border bg-edit-bg/30' : ''
    }`}>
      <div className="relative">
        {/* Boutons d'action - positionnés de manière plus élégante */}
        {editable && (
          <div className="absolute top-3 right-3 z-10">
            {editing ? (
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    // Restaurer les scores initiaux lors de l'annulation
                    setScores([...initialScores]);
                    setEditing(false);
                  }}
                  className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground h-9 rounded-md px-3"
                >
                  <X className="h-3 w-3 mr-1" />
                  Annuler
                </button>
                  <button
                    onClick={async () => {
                      await saveScores();
                      setInitialScores([...scores]);
                      setEditing(false);
                    }}
                  className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 bg-green-600 text-white hover:bg-green-700 h-9 rounded-md px-3 shadow-md"
                >
                  <Save className="h-3 w-3 mr-1" />
                  Sauver
                </button>
              </div>
            ) : (
              <button
                onClick={() => setEditing(true)}
                className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-9 w-9 p-0 hover:bg-primary/10 hover:text-primary"
                title="Modifier les scores"
              >
                <Edit3 className="h-4 w-4" />
              </button>
            )}
          </div>
        )}

        {/* Zone des scores avec padding amélioré */}
        <div className={`divide-y divide-border ${editable ? 'pt-12 pb-4' : 'pt-0'}`}>
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
      </div>
      {stage && (
        <div className="text-center text-xs text-muted-foreground border-t border-border px-4 py-2">
          {stage}
        </div>
      )}
    </div>
  );
}