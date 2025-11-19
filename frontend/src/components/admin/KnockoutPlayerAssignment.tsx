import React, { useMemo, useState } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import GameAssignmentBloc from '@/src/components/admin/GameAssignmentBloc';
import { usePlayerAssignment } from '@/src/hooks/usePlayerAssignment';
import { applyByePositions } from '@/src/utils/byePositioning';

interface Props {
  tournament: Tournament;
  playerPairs: PlayerPair[];
}

export default function KnockoutPlayerAssignment({ tournament, playerPairs }: Props) {
  // Memoize computed values
  const mainDrawSize = (tournament as any)?.config?.mainDrawSize ?? (playerPairs?.length || 0);
  const matchesCount = Math.max(1, Math.floor((mainDrawSize || 0) / 2));
  const slotsSize = matchesCount * 2;

  // Extract first non-GROUPS round stage name
  const firstRoundStageName = React.useMemo(() => {
    const rounds = (tournament as any)?.rounds as Array<any> | undefined;
    if (!Array.isArray(rounds) || rounds.length === 0) {
      return `R${matchesCount}`;
    }

    const firstBracket = rounds.find((r) => r?.stage && r.stage !== 'GROUPS');
    return firstBracket?.stage || `R${matchesCount}`;
  }, [tournament, matchesCount]);

  // Use the hook for drag & drop logic
  const {
    slots,
    dragIndex,
    hoveredIndex,
    matchRefs,
    scrollRef,
    onDragStart,
    onDragOver,
    onDrop,
    onMatchDragOver,
    onMatchDrop,
    onKeyReorder,
    onRootDragOver,
    onRootDragEnd,
  } = usePlayerAssignment(tournament, playerPairs, slotsSize);

  const [applyingByes, setApplyingByes] = useState(false);

  // Apply BYE positions using the public/bye-positions.json mapping
  const applyByes = async () => {
    setApplyingByes(true);
    try {
      const result = await applyByePositions(playerPairs, slotsSize);
      if (result) {
        // Dispatch event to update slots in the hook
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('knockout:apply-byes', { detail: { slots: result } }));
        }
      }
    } finally {
      setApplyingByes(false);
    }
  };

  return (
    <div
      ref={scrollRef}
      className="min-h-[200px] max-h-[80vh] overflow-auto"
      onDragOver={onRootDragOver}
      onDrop={onRootDragEnd}
      onDragEnd={onRootDragEnd}
    >
      <p className="text-sm text-center text-tab-inactive m-2">
        Faites glisser les équipes pour les ordonner.
      </p>
      <div className="flex items-center">
        <div className="h-px flex-1 bg-border my-2" />
        <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none px-4">
          {firstRoundStageName}
        </h3>
        <div className="h-px flex-1 bg-border my-2" />
      </div>

      <div className="rounded-md border border-border bg-card divide-y">
        <div className="p-3 flex justify-center">
          <button
            type="button"
            disabled={applyingByes}
            onClick={applyByes}
            className={`px-3 py-1 rounded text-sm ${applyingByes ? 'bg-border text-muted-foreground cursor-not-allowed' : 'bg-primary text-on-primary hover:bg-primary-hover'}`}
          >
            {applyingByes ? 'Positionnement...' : 'Positionner les BYE'}
          </button>
        </div>
        {Array.from({ length: matchesCount }).map((_, matchIndex) => {
          const slotIndexA = matchIndex * 2;
          const slotIndexB = slotIndexA + 1;
          const pairA = slots[slotIndexA];
          const pairB = slots[slotIndexB];

          return (
            <div
              key={`match-${matchIndex}`}
              ref={(el) => { matchRefs.current[matchIndex] = el; }}
              onDragOver={onMatchDragOver(matchIndex)}
              onDrop={onMatchDrop(matchIndex)}
              className="relative"
            >
              <GameAssignmentBloc
                title={`Match ${matchIndex + 1}/${matchesCount}`}
                pairA={pairA}
                pairB={pairB}
                handlersA={{
                  onDragStart: onDragStart(slotIndexA),
                  onDragOver: onDragOver(slotIndexA),
                  onDrop: onDrop(slotIndexA),
                  onMoveUp: onKeyReorder(slotIndexA, -1),
                  onMoveDown: onKeyReorder(slotIndexA, 1)
                }}
                handlersB={{
                  onDragStart: onDragStart(slotIndexB),
                  onDragOver: onDragOver(slotIndexB),
                  onDrop: onDrop(slotIndexB),
                  onMoveUp: onKeyReorder(slotIndexB, -1),
                  onMoveDown: onKeyReorder(slotIndexB, 1)
                }}
                isActiveA={dragIndex === slotIndexA}
                isActiveB={dragIndex === slotIndexB}
                isOverA={hoveredIndex === slotIndexA}
                isOverB={hoveredIndex === slotIndexB}
              />
            </div>
          );
        })}
      </div>
    </div>
  );
}