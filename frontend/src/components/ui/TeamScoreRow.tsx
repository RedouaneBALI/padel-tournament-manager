'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import TeamRow from '@/src/components/ui/TeamRow';
import { Flag } from 'lucide-react';

function focusByTabIndex(nextIndex: number) {
  const el = document.querySelector<HTMLInputElement>(`input[tabindex="${nextIndex}"]`);
  if (el) {
    el.focus();
    el.select?.();
  }
}

interface Props {
  team: PlayerPair | null;
  teamIndex: number;
  scores: string[];
  editing: boolean;
  setScores?: (scores: string[]) => void;
  inputRefs?: React.MutableRefObject<(HTMLInputElement | null)[]>;
  handleKeyDown?: (e: React.KeyboardEvent, teamIndex: number, setIndex: number) => void;
  winnerSide?: number;
  visibleSets?: number;
  computeTabIndex?: (teamIndex: number, setIndex: number) => number;
  forfeited?: boolean;
  onToggleForfeit?: () => void;
}

export default function TeamScoreRow({
  team,
  teamIndex,
  scores,
  editing,
  setScores,
  inputRefs,
  handleKeyDown,
  winnerSide,
  visibleSets,
  computeTabIndex,
  forfeited,
  onToggleForfeit,
}: Props) {
  const handleChange = (setIndex: number, value: string) => {
    const sanitized = value.replace(/[^0-9]/g, '');
    const updated = [...scores];
    updated[setIndex] = sanitized;
    setScores?.(updated);
  };

  const onKeyDownLocal = (e: React.KeyboardEvent, tIdx: number, sIdx: number) => {
    handleKeyDown?.(e, tIdx, sIdx);
  };

  // Détermine le nombre de sets à afficher (par défaut 3, mais peut être dynamique)
  const setsCount = visibleSets ?? scores.length;

  return (
    <div className="flex items-center px-4 h-[60px]">
      <TeamRow team={team} winnerSide={winnerSide} teamIndex={teamIndex} />

      <div className="flex items-center gap-2 ml-4">
        {editing ? (
          <>
            {/* Bouton AB compact à gauche des scores */}
            <button
              type="button"
              onClick={onToggleForfeit}
              className={`flex items-center justify-center rounded border border-border text-xs font-bold transition-colors ${forfeited ? 'bg-yellow-500/20 text-yellow-700 border-yellow-500/30' : 'bg-muted/30 text-muted-foreground'}`}
              title="Abandon"
              style={{ padding: 0, minWidth: '2rem', minHeight: '1rem' }}
            >
              <Flag className="h-2 w-2" />
              <span className={forfeited ? 'text-[10px] font-bold' : 'text-[10px] font-normal text-muted-foreground'}>AB</span>
            </button>
            <div className="flex space-x-1">
              {scores.slice(0, visibleSets ?? 3).map((setScore, setIndex) => (
                <input
                  key={setIndex}
                  type="tel"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  value={setScore}
                  ref={(el) => {
                    if (inputRefs) inputRefs.current[setIndex] = el;
                  }}
                  onChange={(e) => handleChange(setIndex, e.target.value)}
                  onKeyDown={(e) => onKeyDownLocal(e, teamIndex, setIndex)}
                  className="w-8 text-xs text-center border border-border rounded"
                  placeholder="-"
                  tabIndex={computeTabIndex ? computeTabIndex(teamIndex, setIndex) : undefined}
                  enterKeyHint="next"
                />
              ))}
            </div>
          </>
        ) : (
          <div className={`grid grid-cols-${setsCount + 1} justify-items-end text-base font-semibold text-foreground w-full`} style={{ gridTemplateColumns: `min-content repeat(${setsCount}, minmax(2ch, auto))` }}>
            {/* Colonne AB à gauche */}
            <span className="min-w-[2ch] text-center">
              {forfeited ? (
                <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-yellow-500/20 text-yellow-700 dark:text-yellow-300 border border-yellow-500/30 mr-1">
                  AB
                </span>
              ) : null}
            </span>
            {/* Scores, alignés à droite, pas de colonne vide inutile */}
            {Array.from({ length: setsCount }).map((_, i) => (
              <span key={i} className="min-w-[2ch] text-right">{scores[i] || ''}</span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
