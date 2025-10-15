import React, { useMemo } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import GameAssignmentBloc from '@/src/components/admin/GameAssignmentBloc';
import { useDualPlayerAssignment } from '@/src/hooks/useDualPlayerAssignment';

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