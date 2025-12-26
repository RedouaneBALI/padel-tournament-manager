'use client';

import React, { useEffect, useRef, useState, useMemo } from 'react';
import MatchResultCardLight from '@/src/components/ui/MatchResultCardLight';
import type { Round } from '@/src/types/round';
import { calculateMatchPositions } from '@/src/utils/bracket';

interface KnockoutBracketProps {
  rounds: Round[];
  tournamentId: string;
  isQualif: boolean;
  hideBye?: boolean;
}

function getCenterY(el: HTMLElement, containerRect: DOMRect): number {
  try {
    const divide = el.querySelector('.divide-y, .divide-gray-200, .divide-border');
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
      return dividerCenter - containerRect.top;
    } else {
      const rect = el.getBoundingClientRect();
      return rect.top - containerRect.top + rect.height / 2;
    }
  } catch (e) {
    const rect = el.getBoundingClientRect();
    return rect.top - containerRect.top + rect.height / 2;
  }
}

function calculateChildCoords(childIdx: number, rounds: Round[], r: number, ROUND_WIDTH: number, matchPositions: number[][], nodeRefs: React.MutableRefObject<Map<string, HTMLElement>>, containerRect: DOMRect): { cx: number; cy: number } | null {
  const childGame = rounds[r].games[childIdx];
  if (!childGame) return null;
  const childId = String(childGame.id);
  const childEl = nodeRefs.current.get(childId);

  let cx = r * ROUND_WIDTH + ROUND_WIDTH - 10; // default right inside column
  let cy = (matchPositions[r]?.[childIdx] ?? 0) + 35 + 60;

  if (childEl) {
    const cRect = childEl.getBoundingClientRect();
    cx = cRect.right - containerRect.left;
    cy = getCenterY(childEl, containerRect);
  }
  return { cx, cy };
}

function computeConnections(rounds: Round[], ROUND_WIDTH: number, matchPositions: number[][], nodeRefs: React.MutableRefObject<Map<string, HTMLElement>>, containerRect: DOMRect, hideBye: boolean): Array<{ cx1:number, cy1:number, cx2:number, cy2:number, px:number, py:number }> {
  const conns: Array<{ cx1:number, cy1:number, cx2:number, cy2:number, px:number, py:number }> = [];

  for (let r = 0; r < rounds.length - 1; r++) {
    const current = rounds[r];
    const next = rounds[r + 1];

    next.games.forEach((parentGame, parentIndex) => {
      // Skip if parent is BYE and hideBye is true
      if (hideBye && (parentGame.teamA?.type === 'BYE' || parentGame.teamB?.type === 'BYE')) return;

      // children indices in previous round
      const childIdxA = parentIndex * 2;
      const childIdxB = parentIndex * 2 + 1;

      const parentId = String(parentGame.id);

      const parentEl = nodeRefs.current.get(parentId);
      // parent coords (relative to container) - left edge and center Y
      let px = (r + 1) * ROUND_WIDTH + 10; // fallback left inside column
      let py = (matchPositions[r + 1]?.[parentIndex] ?? 0) + 35 + 60; // fallback center Y
      if (parentEl) {
        const pRect = parentEl.getBoundingClientRect();
        px = pRect.left - containerRect.left; // left edge
        py = getCenterY(parentEl, containerRect);
      }

      // children
      const childCoords: Array<{cx:number, cy:number}> = [];
      [childIdxA, childIdxB].forEach((childIdx) => {
        const childGame = rounds[r].games[childIdx];
        // Skip BYE children if hideBye is true
        if (childGame && (!hideBye || !(childGame.teamA?.type === 'BYE' || childGame.teamB?.type === 'BYE'))) {
          const coords = calculateChildCoords(childIdx, rounds, r, ROUND_WIDTH, matchPositions, nodeRefs, containerRect);
          if (coords) childCoords.push(coords);
        }
      });

      // Create connection if we have 2 children, or if we have 1 child and hideBye is true (the other is a BYE)
      if (childCoords.length === 2) {
        conns.push({ cx1: childCoords[0].cx, cy1: childCoords[0].cy, cx2: childCoords[1].cx, cy2: childCoords[1].cy, px, py });
      } else if (childCoords.length === 1 && hideBye) {
        // Single child when hideBye - still connect it to the parent
        conns.push({ cx1: childCoords[0].cx, cy1: childCoords[0].cy, cx2: childCoords[0].cx, cy2: childCoords[0].cy, px, py });
      }
    });
  }

  return conns;
}

export default function KnockoutBracket({ rounds, tournamentId, isQualif, hideBye }: KnockoutBracketProps) {
  const ROUND_WIDTH = 320;
  const CONNECTOR_STROKE = 1.5; // px used for SVG stroke width

  // Update: pass hideBye to the calculation
  const matchPositions = useMemo(() => calculateMatchPositions(rounds, hideBye), [rounds, hideBye]);

  const maxPosition = Math.max(...matchPositions.flat()) + 150;

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

      const conns = computeConnections(rounds, ROUND_WIDTH, matchPositions, nodeRefs, containerRect, !!hideBye);

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
  }, [rounds, matchPositions, hideBye]);

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
          // Check if this is a single child connection (cx1 === cx2 and cy1 === cy2)
          const isSingleChild = c.cx1 === c.cx2 && c.cy1 === c.cy2;

          if (isSingleChild) {
            // Single child: draw direct line from child to parent with orthogonal angles
            const midX = (c.cx1 + c.px) / 2;
            return (
              <g key={i} stroke="rgba(148,163,184,0.45)" strokeWidth={CONNECTOR_STROKE} fill="none" strokeLinecap="round">
                {/* horizontal from child to midpoint */}
                <line x1={c.cx1} y1={c.cy1} x2={midX} y2={c.cy1} />
                {/* vertical from child height to parent height */}
                <line x1={midX} y1={c.cy1} x2={midX} y2={c.py} />
                {/* horizontal from midpoint to parent */}
                <line x1={midX} y1={c.py} x2={c.px} y2={c.py} />
              </g>
            );
          } else {
            // Two children: normal bracket connection
            const midX = Math.round((Math.max(c.cx1, c.cx2) + c.px) / 2);
            const minY = Math.min(c.cy1, c.cy2);
            const maxY = Math.max(c.cy1, c.cy2);
            const midY = c.py;

            return (
              <g key={`${c.cx1}-${c.cy1}-${c.cx2}-${c.cy2}-${c.px}-${c.py}`} stroke="rgba(148,163,184,0.45)" strokeWidth={CONNECTOR_STROKE} fill="none" strokeLinecap="round">
                {/* horizontals from children to midX */}
                <line x1={c.cx1} y1={c.cy1} x2={midX} y2={c.cy1} />
                <line x1={c.cx2} y1={c.cy2} x2={midX} y2={c.cy2} />
                {/* vertical between children at midX */}
                <line x1={midX} y1={minY} x2={midX} y2={maxY} />
                {/* horizontal from midY to parent left */}
                <line x1={midX} y1={midY} x2={c.px} y2={midY} />
              </g>
            );
          }
        })}
      </svg>

      {rounds.map((round, roundIndex) => (
        <div key={round.id} className="relative" style={{ width: ROUND_WIDTH }}>
          <div className="absolute top-0 left-0 right-0 text-center mb-4 text-sm font-semibold border-b-2 border-primary text-primary">
            {round.stage}
          </div>

          {round.games.map((game, gameIndex) => {
            const isBye = game.teamA?.type === 'BYE' || game.teamB?.type === 'BYE';
            // Important: we still rely on matchPositions even if hidden logic is complex
            // matchPositions now accounts for compacting.
            return (
              <div
                key={game.id}
                ref={(el) => {
                  if (el) nodeRefs.current.set(String(game.id), el);
                  else nodeRefs.current.delete(String(game.id));
                }}
                className="absolute"
                style={{
                  top: `${matchPositions[roundIndex][gameIndex] + 35}px`,
                  left: '10px',
                  right: '10px',
                }}
              >
                {(!hideBye || !isBye) && (
                  <MatchResultCardLight
                    teamA={game.teamA}
                    teamB={game.teamB}
                    score={game.score}
                    winnerSide={game.finished ? game.winnerSide : undefined}
                    finished={game.finished}
                    stage={round.stage}
                    scheduledTime={game.scheduledTime}
                  />
                )}
              </div>
            );
          })}

          {isQualif && roundIndex === rounds.length - 1 && round.games.map((game, gameIndex) => {
            const isBye = game.teamA?.type === 'BYE' || game.teamB?.type === 'BYE';
            if (hideBye && isBye) return null;

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
                  top: `calc(${matchPositions[roundIndex][gameIndex] + 30}px + ${(matchHeight / 2)}px)`,
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