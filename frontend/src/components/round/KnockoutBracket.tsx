'use client';

import React, { useEffect, useRef, useState, useMemo } from 'react';
import MatchResultCardLight from '@/src/components/ui/MatchResultCardLight';
import type { Round } from '@/src/types/round';
import { calculateMatchPositions } from '@/src/utils/bracket';

interface KnockoutBracketProps {
  rounds: Round[];
  tournamentId: string;
  isQualif: boolean;
}

function calculateChildCoords(childIdx: number, rounds: Round[], r: number, ROUND_WIDTH: number, matchPositions: number[][], nodeRefs: React.MutableRefObject<Map<string, HTMLElement>>, containerRect: DOMRect): { cx: number; cy: number } | null {
  const childGame = rounds[r].games[childIdx];
  if (!childGame) return null;
  const childId = String(childGame.id);
  const childEl = nodeRefs.current.get(childId);

  let cx = r * ROUND_WIDTH + ROUND_WIDTH - 10; // default right inside column
  let cy = (matchPositions[r]?.[childIdx] ?? 0) + 40 + 60;

  if (childEl) {
    const cRect = childEl.getBoundingClientRect();
    cx = cRect.right - containerRect.left;
    // Try to align on the divider between the two TeamScoreRow children if present
    try {
      const divide = childEl.querySelector('.divide-y, .divide-gray-200, .divide-border');
      if (divide && divide.children && divide.children.length >= 1) {
        const firstRow = divide.children[0] as HTMLElement;
        const firstRect = firstRow.getBoundingClientRect();
        const style = window.getComputedStyle(firstRow);
        const borderBottom = parseFloat(style.borderBottomWidth || '0') || 0;
        const borderTop = parseFloat(style.borderTopWidth || '0') || 0;
        let dividerCenter: number;
        if (borderBottom > 0) {
          dividerCenter = firstRect.bottom - borderBottom / 2;
        } else if (borderTop > 0) {
          dividerCenter = firstRect.top + borderTop / 2;
        } else {
          dividerCenter = firstRect.top + firstRect.height / 2;
        }
        // Align directly on the divider center (no extra offset)
        cy = dividerCenter - containerRect.top;
      } else {
        cy = cRect.top - containerRect.top + cRect.height / 2;
      }
    } catch (e) {
      cy = cRect.top - containerRect.top + cRect.height / 2;
    }
  }
  return { cx, cy };
}

function computeConnections(rounds: Round[], ROUND_WIDTH: number, matchPositions: number[][], nodeRefs: React.MutableRefObject<Map<string, HTMLElement>>, containerRect: DOMRect): Array<{ cx1:number, cy1:number, cx2:number, cy2:number, px:number, py:number }> {
  const conns: Array<{ cx1:number, cy1:number, cx2:number, cy2:number, px:number, py:number }> = [];

  for (let r = 0; r < rounds.length - 1; r++) {
    const current = rounds[r];
    const next = rounds[r + 1];

    next.games.forEach((parentGame, parentIndex) => {
      // children indices in previous round
      const childIdxA = parentIndex * 2;
      const childIdxB = parentIndex * 2 + 1;

      const parentId = String(parentGame.id);

      const parentEl = nodeRefs.current.get(parentId);
      // parent coords (relative to container) - left edge and center Y
      let px = (r + 1) * ROUND_WIDTH + 10; // fallback left inside column
      let py = (matchPositions[r + 1]?.[parentIndex] ?? 0) + 40 + 60; // fallback center Y
      if (parentEl) {
        const pRect = parentEl.getBoundingClientRect();
        px = pRect.left - containerRect.left; // left edge
        // Try to align parent connector on the internal divider between TeamScoreRow
        try {
          const divideP = parentEl.querySelector('.divide-y, .divide-gray-200, .divide-border');
          if (divideP && divideP.children && divideP.children.length >= 1) {
            // prefer using a first row's bottom border center if available
            const firstRowP = divideP.children[0] as HTMLElement;
            const firstRectP = firstRowP.getBoundingClientRect();
            const styleP = window.getComputedStyle(firstRowP);
            const borderBottomP = parseFloat(styleP.borderBottomWidth || '0') || 0;
            const borderTopP = parseFloat(styleP.borderTopWidth || '0') || 0;
            let dividerCenterP: number;
            if (borderBottomP > 0) {
              dividerCenterP = firstRectP.bottom - borderBottomP / 2;
            } else if (borderTopP > 0) {
              dividerCenterP = firstRectP.top + borderTopP / 2;
            } else {
              dividerCenterP = firstRectP.top + firstRectP.height / 2;
            }
            // Align directly on the divider center (no extra offset) to avoid over/under correction
            py = dividerCenterP - containerRect.top;
          } else {
            py = pRect.top - containerRect.top + pRect.height / 2;
          }
        } catch (e) {
          py = pRect.top - containerRect.top + pRect.height / 2;
        }
      }

      // children
      const childCoords: Array<{cx:number, cy:number}> = [];
      [childIdxA, childIdxB].forEach((childIdx) => {
        const coords = calculateChildCoords(childIdx, rounds, r, ROUND_WIDTH, matchPositions, nodeRefs, containerRect);
        if (coords) childCoords.push(coords);
      });

      if (childCoords.length === 2) {
        conns.push({ cx1: childCoords[0].cx, cy1: childCoords[0].cy, cx2: childCoords[1].cx, cy2: childCoords[1].cy, px, py });
      }
    });
  }

  return conns;
}

export default function KnockoutBracket({ rounds, tournamentId, isQualif }: KnockoutBracketProps) {
  const ROUND_WIDTH = 320;
  const CONNECTOR_STROKE = 1.5; // px used for SVG stroke width
  const matchPositions = useMemo(() => calculateMatchPositions(rounds), [rounds]);
  const maxPosition = Math.max(...matchPositions.flat()) + 200;

  // refs
  const containerRef = useRef<HTMLDivElement | null>(null);
  const nodeRefs = useRef<Map<string, HTMLDivElement>>(new Map());

  // connections: array of orthogonal segment groups {cx1,cy1,cx2,cy2, px,py}
  const [connections, setConnections] = useState<Array<{ cx1:number, cy1:number, cx2:number, cy2:number, px:number, py:number }>>([]);
  const lastConnsRef = useRef<string | null>(null);

  // compute connections after render / on resize
  useEffect(() => {
    function compute() {
      const container = containerRef.current;
      if (!container) return;
      const containerRect = container.getBoundingClientRect();

      const conns = computeConnections(rounds, ROUND_WIDTH, matchPositions, nodeRefs, containerRect);

      // Only update state when connections actually changed to avoid infinite loops
      try {
        const key = JSON.stringify(conns.map(c => [c.cx1,c.cy1,c.cx2,c.cy2,c.px,c.py]));
        if (lastConnsRef.current !== key) {
          lastConnsRef.current = key;
          setConnections(conns);
        }
      } catch (e) {
        // fallback: set anyway
        setConnections(conns);
      }
    }

    compute();
    window.addEventListener('resize', compute);
    const observer = new MutationObserver(compute);
    if (containerRef.current) observer.observe(containerRef.current, { childList: true, subtree: true });
    return () => {
      window.removeEventListener('resize', compute);
      observer.disconnect();
    };
  }, [rounds, matchPositions]);

  return (
    <div
      ref={containerRef}
      className="relative flex"
      style={{
        width: `${rounds.length * ROUND_WIDTH + (isQualif ? 50 : 0)}px`,
        height: `${maxPosition}px`,
      }}
    >
      {/* SVG overlay for orthogonal connections */}
      <svg className="absolute inset-0 pointer-events-none" width={rounds.length * ROUND_WIDTH + (isQualif ? 50 : 0)} height={maxPosition}>
        {connections.map((c, i) => {
          const midX = Math.round((Math.max(c.cx1, c.cx2) + c.px) / 2);
          const minY = Math.min(c.cy1, c.cy2);
          const maxY = Math.max(c.cy1, c.cy2);
          // Use parent's computed connector Y so the final horizontal aligns with the parent's internal divider
          const midY = c.py; // keep float to avoid 1px rounding shifts

            return (
            <g key={i} stroke="rgba(148,163,184,0.45)" strokeWidth={CONNECTOR_STROKE} fill="none" strokeLinecap="round">
                {/* horizontals from children to midX */}
                <line x1={c.cx1} y1={c.cy1} x2={midX} y2={c.cy1} />
                <line x1={c.cx2} y1={c.cy2} x2={midX} y2={c.cy2} />
                {/* vertical between children at midX */}
                <line x1={midX} y1={minY} x2={midX} y2={maxY} />
                {/* horizontal from midY to parent left */}
               <line x1={midX} y1={midY} x2={c.px} y2={midY} />
              </g>
            );
          })}
      </svg>

      {rounds.map((round, roundIndex) => (
        <div key={round.id} className="relative" style={{ width: ROUND_WIDTH }}>
          <div className="absolute top-0 left-0 right-0 text-center mb-4 text-sm font-semibold border-b-2 border-primary text-primary">
            {round.stage}
          </div>

          {round.games.map((game, gameIndex) => (
            <div
              key={game.id}
              ref={(el) => {
                if (el) nodeRefs.current.set(String(game.id), el);
                else nodeRefs.current.delete(String(game.id));
              }}
              className="absolute"
              style={{
                top: `${matchPositions[roundIndex][gameIndex] + 40}px`,
                left: '10px',
                right: '10px',
              }}
            >
              <MatchResultCardLight
                teamA={game.teamA}
                teamB={game.teamB}
                score={game.score}
                winnerSide={
                  game.finished
                    ? game.winnerSide === 'TEAM_A'
                      ? 0
                      : game.winnerSide === 'TEAM_B'
                        ? 1
                        : undefined
                    : undefined
                }
                finished={game.finished}
                stage={round.stage}
                scheduledTime={game.scheduledTime}
              />
            </div>
          ))}

          {isQualif && roundIndex === rounds.length - 1 && round.games.map((game, gameIndex) => {
            // Get the ref to the MatchResultCardLight DOM node
            const matchNode = nodeRefs.current.get(String(game.id));
            let matchHeight = 0;
            if (matchNode) {
              matchHeight = matchNode.offsetHeight;
            } else {
              matchHeight = 120; // fallback default
            }
            return (
              <div
                key={`label-${game.id}`}
                className="absolute text-sm flex items-center"
                style={{
                  top: `calc(${matchPositions[roundIndex][gameIndex] + 40}px + ${(matchHeight / 2)}px)`,
                  left: `${ROUND_WIDTH + 10}px`,
                  transform: 'translateY(-50%)',
                  height: `${matchHeight}px`,
                  minHeight: '40px',
                }}
              >
                Q{gameIndex + 1}
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}
