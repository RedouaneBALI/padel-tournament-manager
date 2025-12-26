import React, { useMemo, useState } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import GameAssignmentBloc from '@/src/components/admin/GameAssignmentBloc';
import { useDualPlayerAssignment } from '@/src/hooks/useDualPlayerAssignment';
import { applyByePositions } from '@/src/utils/byePositioning';

interface Props {
  tournament: Tournament;
  playerPairs: PlayerPair[];
}

export default function QualifKoPlayerAssignment({ tournament, playerPairs }: Props) {
  // Calculate sizes for qualif and main draw from tournament config
  const qualifSize = (tournament as any)?.config?.preQualDrawSize ?? Math.floor(playerPairs.length / 4);
  const mainSize = (tournament as any)?.config?.mainDrawSize ?? (playerPairs.length - qualifSize);
  const qualifMatchesCount = Math.max(1, Math.floor(qualifSize / 2));
  const mainMatchesCount = Math.max(1, Math.floor(mainSize / 2));
  const qualifSlotsSize = qualifMatchesCount * 2;
  const mainSlotsSize = mainMatchesCount * 2;

  // Use the dual hook
  const {
    qualifSlots,
    mainSlots,
    dragIndex,
    dragList,
    hoveredIndex,
    hoveredList,
    qualifRefs,
    mainRefs,
    scrollRef,
    onDragStart,
    onDragOver,
    onDrop,
    onMatchDragOver,
    onMatchDrop,
    onKeyReorder,
    onRootDragOver,
    onRootDragEnd,
  } = useDualPlayerAssignment(tournament, playerPairs, qualifSlotsSize, mainSlotsSize);

  const [applyingByes, setApplyingByes] = useState(false);
  const [applyingQualifByes, setApplyingQualifByes] = useState(false);

  // Check if there are any BYE pairs in the qualif draw
  const hasQualifByePairs = React.useMemo(() => {
    return qualifSlots.some((p) => p?.type === 'BYE');
  }, [qualifSlots]);

  // Check if there are any BYE pairs in the main draw
  const hasMainDrawByePairs = React.useMemo(() => {
    return mainSlots.some((p) => p?.type === 'BYE');
  }, [mainSlots]);

  // Apply BYE positions for qualif draw
  const applyQualifByes = async () => {
    setApplyingQualifByes(true);
    try {
      const qualifDrawPairs = qualifSlots.filter(Boolean) as PlayerPair[];
      const result = await applyByePositions(qualifDrawPairs, qualifSlotsSize);
      if (result) {
        // Dispatch event to update qualif slots
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('qualifko:apply-qualif-byes', { detail: { qualifSlots: result } }));
        }
      }
    } finally {
      setApplyingQualifByes(false);
    }
  };

  // Apply BYE positions for main draw
  const applyByes = async () => {
    setApplyingByes(true);
    try {
      const mainDrawPairs = mainSlots.filter(Boolean) as PlayerPair[];
      const result = await applyByePositions(mainDrawPairs, mainSlotsSize);
      if (result) {
        // Dispatch event to update main slots
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('qualifko:apply-main-byes', { detail: { mainSlots: result } }));
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
        Faites glisser les équipes entre les tableaux pour les ordonner.
      </p>
      {/* Qualif Section */}
      <div className="mb-8">
        <div className="flex items-center">
          <div className="h-px flex-1 bg-border my-2" />
          <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none px-4">
            Qualifications
          </h3>
          <div className="h-px flex-1 bg-border my-2" />
        </div>
        <div className="rounded-md border border-border bg-card divide-y">
          {hasQualifByePairs && (
            <div className="p-3 flex justify-center">
              <button
                type="button"
                disabled={applyingQualifByes}
                onClick={applyQualifByes}
                className={`px-3 py-1 rounded text-sm ${applyingQualifByes ? 'bg-border text-muted-foreground cursor-not-allowed' : 'bg-primary text-on-primary hover:bg-primary-hover'}`}
              >
                {applyingQualifByes ? 'Positionnement...' : 'Positionner les BYE'}
              </button>
            </div>
          )}
          {Array.from({ length: qualifMatchesCount }).map((_, matchIndex) => {
            const slotIndexA = matchIndex * 2;
            const slotIndexB = slotIndexA + 1;
            const pairA = qualifSlots[slotIndexA];
            const pairB = qualifSlots[slotIndexB];

            return (
              <div
                key={`qualif-match-${matchIndex}`}
                ref={(el) => { qualifRefs.current[matchIndex] = el; }}
                onDragOver={onMatchDragOver('qualif', matchIndex)}
                onDrop={onMatchDrop('qualif', matchIndex)}
                className="relative"
              >
                <GameAssignmentBloc
                  title={`Match ${matchIndex + 1}/${qualifMatchesCount}`}
                  pairA={pairA}
                  pairB={pairB}
                  handlersA={{
                    onDragStart: onDragStart('qualif', slotIndexA),
                    onDragOver: onDragOver('qualif', slotIndexA),
                    onDrop: onDrop('qualif', slotIndexA),
                    onMoveUp: onKeyReorder('qualif', slotIndexA, -1),
                    onMoveDown: onKeyReorder('qualif', slotIndexA, 1)
                  }}
                  handlersB={{
                    onDragStart: onDragStart('qualif', slotIndexB),
                    onDragOver: onDragOver('qualif', slotIndexB),
                    onDrop: onDrop('qualif', slotIndexB),
                    onMoveUp: onKeyReorder('qualif', slotIndexB, -1),
                    onMoveDown: onKeyReorder('qualif', slotIndexB, 1)
                  }}
                  isActiveA={dragList === 'qualif' && dragIndex === slotIndexA}
                  isActiveB={dragList === 'qualif' && dragIndex === slotIndexB}
                  isOverA={hoveredList === 'qualif' && hoveredIndex === slotIndexA}
                  isOverB={hoveredList === 'qualif' && hoveredIndex === slotIndexB}
                />
              </div>
            );
          })}
        </div>
      </div>
      {/* Main Draw Section */}
      <div>
        <div className="flex items-center">
          <div className="h-px flex-1 bg-border my-2" />
          <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none px-4">
            Tableau Principal
          </h3>
          <div className="h-px flex-1 bg-border my-2" />
        </div>

        <div className="rounded-md border border-border bg-card divide-y">
          {hasMainDrawByePairs && (
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
          )}
          {Array.from({ length: mainMatchesCount }).map((_, matchIndex) => {
            const slotIndexA = matchIndex * 2;
            const slotIndexB = slotIndexA + 1;
            const pairA = mainSlots[slotIndexA];
            const pairB = mainSlots[slotIndexB];

            return (
              <div
                key={`main-match-${matchIndex}`}
                ref={(el) => { mainRefs.current[matchIndex] = el; }}
                onDragOver={onMatchDragOver('main', matchIndex)}
                onDrop={onMatchDrop('main', matchIndex)}
                className="relative"
              >
                <GameAssignmentBloc
                  title={`Match ${matchIndex + 1}/${mainMatchesCount}`}
                  pairA={pairA}
                  pairB={pairB}
                  handlersA={{
                    onDragStart: onDragStart('main', slotIndexA),
                    onDragOver: onDragOver('main', slotIndexA),
                    onDrop: onDrop('main', slotIndexA),
                    onMoveUp: onKeyReorder('main', slotIndexA, -1),
                    onMoveDown: onKeyReorder('main', slotIndexA, 1)
                  }}
                  handlersB={{
                    onDragStart: onDragStart('main', slotIndexB),
                    onDragOver: onDragOver('main', slotIndexB),
                    onDrop: onDrop('main', slotIndexB),
                    onMoveUp: onKeyReorder('main', slotIndexB, -1),
                    onMoveDown: onKeyReorder('main', slotIndexB, 1)
                  }}
                  isActiveA={dragList === 'main' && dragIndex === slotIndexA}
                  isActiveB={dragList === 'main' && dragIndex === slotIndexB}
                  isOverA={hoveredList === 'main' && hoveredIndex === slotIndexA}
                  isOverB={hoveredList === 'main' && hoveredIndex === slotIndexB}
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}