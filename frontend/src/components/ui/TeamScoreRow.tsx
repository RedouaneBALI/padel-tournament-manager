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
  showAbSlot?: boolean;
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
  showAbSlot,
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

  // Configuration d'affichage : largeur d'une "cellule" de score
  // On la réduit légèrement pour resserrer l'espace entre les scores (w-4 ~ 1rem)
  const CELL_WIDTH_REM = 1; // correspond à w-4
  const CELL_GAP_REM = 0.125; // correspond à gap-0.5
  // On fixe la grille à 3 colonnes visibles maximum pour garder l'alignement à droite
  const MAX_DISPLAY_SETS = 3;
  // largeur de la zone AB en rem (choisie pour contenir la boîte du badge AB)
  const AB_SLOT_WIDTH_REM = 1.15; // taille plus compacte pour éviter trop d'espace à droite
  // container width includes AB slot and a small gap before it
  const containerWidthRem = MAX_DISPLAY_SETS * CELL_WIDTH_REM + (MAX_DISPLAY_SETS - 1) * CELL_GAP_REM + AB_SLOT_WIDTH_REM + 0.25;

  return (
    // rendre relatif pour positionner le badge AB en absolute sans impacter le flux
    <div className="relative flex items-center pl-4 pr-2 h-[60px]">
      {/* Bloc des noms qui prend tout l'espace disponible */}
      <div className="flex-1 min-w-0">
        <TeamRow team={team} winnerSide={winnerSide} teamIndex={teamIndex} />
      </div>

      {/* Edition: conserver le rendu inline pour l'édition */}
      {editing ? (
        <div className="flex items-center gap-2 ml-4">
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
                className="w-8 text-xs text-center border border-border rounded tabular-nums"
                placeholder="-"
                tabIndex={computeTabIndex ? computeTabIndex(teamIndex, setIndex) : undefined}
                enterKeyHint="next"
              />
            ))}
          </div>
        </div>
      ) : (
        // Affichage normal : scores alignés à droite, AB (colonne) dans le flux uniquement si showAbSlot
        (() => {
          const baseWidthRem = setsCount * CELL_WIDTH_REM + Math.max(0, setsCount - 1) * CELL_GAP_REM;
          // symmetric gap around AB: on utilise GAP_AROUND_AB (rem)
          const GAP_AROUND_AB = CELL_GAP_REM; // 0.125rem
          const abReservedRem = showAbSlot ? (2 * GAP_AROUND_AB + AB_SLOT_WIDTH_REM) : 0;
          const containerWidthRem = baseWidthRem + abReservedRem;

          // construire grid columns string: scores columns + (gap + AB + gap) si showAbSlot
          const scoresCols = Array.from({ length: setsCount }).map(() => `${CELL_WIDTH_REM}rem`).join(' ');

          const gridTemplate = showAbSlot
            ? `${scoresCols} ${GAP_AROUND_AB}rem ${AB_SLOT_WIDTH_REM}rem ${GAP_AROUND_AB}rem`
            : `${scoresCols}`;

          return (
            <div className="ml-auto" style={{ width: `${containerWidthRem}rem`, display: 'grid', gridTemplateColumns: gridTemplate, columnGap: `${CELL_GAP_REM}rem`, alignItems: 'center' }}>
              {Array.from({ length: setsCount }).map((_, i) => {
                const isWinner = winnerSide !== undefined && winnerSide === teamIndex;
                return (
                  <div key={`score-${i}`} className={`text-center tabular-nums text-base leading-none ${isWinner ? 'font-bold' : 'font-semibold'}`} style={{ justifySelf: 'center' }}>{scores[i] || ''}</div>
                );
              })}

              {showAbSlot && (
                // gap left cell (empty) will be created by grid template; now AB cell
                <div key="ab-slot" style={{ width: `${AB_SLOT_WIDTH_REM}rem`, justifySelf: 'center', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {forfeited ? (
                    <span className="inline-flex items-center justify-center h-5 text-[10px] font-bold px-1 py-0.5 rounded bg-yellow-500/20 text-yellow-700 dark:text-yellow-300 border border-yellow-500/30 leading-none">
                      AB
                    </span>
                  ) : (
                    <span className="inline-flex items-center justify-center h-5 px-1 py-0.5 rounded border border-transparent bg-transparent text-transparent leading-none">&nbsp;</span>
                  )}
                </div>
              )}
            </div>
          );
        })()
      )}
    </div>
  );
}
