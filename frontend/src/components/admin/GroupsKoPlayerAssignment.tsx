import React from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';

interface Props {
  tournament: Tournament;
  playerPairs: PlayerPair[];
}

export default function GroupsKoPlayerAssignment({ tournament, playerPairs }: Props) {
  return (
    <div className="min-h-[200px]">
      <h3 className="text-base font-medium mb-2">Affectation – Poules puis élimination</h3>
      <p className="text-sm text-tab-inactive">{playerPairs.length} équipes chargées</p>
    </div>
  );
}
