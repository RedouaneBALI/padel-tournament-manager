// components/MatchResultCardLight.tsx

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';

interface MatchResultCardLightProps {
  teamA: PlayerPair;
  teamB: PlayerPair;
}

export default function MatchResultCardLight({ teamA, teamB }: MatchResultCardLightProps) {
  return (
    <div className="rounded-lg bg-white shadow-sm border border-gray-200/80 p-3.5">
      {[teamA, teamB].map((team, idx) => (
        <div key={idx} className={`flex justify-between items-center py-1 ${idx === 0 ? '' : 'border-t border-gray-100 mt-1 pt-2'}`}>
          <div className="space-y-1">
            <p className="text-sm text-gray-800 truncate">
              {team.player1.name}
            </p>
            <p className="text-sm text-gray-800 truncate">
              {team.player2.name}
            </p>
          </div>
          {team.seed != null && (
            <span className="text-xs text-gray-500 ml-2 whitespace-nowrap">({team.seed})</span>
          )}
        </div>
      ))}
    </div>
  );
}