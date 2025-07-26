// components/MatchResultCardLight.tsx

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Game } from '@/src/types/game';

export default function MatchResultCardLight(game: Game) {
  return (
    <div className="rounded-lg bg-white shadow-sm border border-gray-200/80 p-3.5">
      {[game.teamA, game.teamB].map((team, idx) => (
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