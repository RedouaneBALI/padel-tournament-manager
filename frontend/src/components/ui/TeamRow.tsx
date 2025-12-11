// src/components/ui/TeamRow.tsx
'use client';

import React from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Trophy } from 'lucide-react';
import { cn } from '@/src/lib/utils';

interface Props {
  team: PlayerPair | null;
  winnerSide?: number;
  teamIndex?: number; // 0 ou 1
  showChampion?: boolean;
  fontSize?: string;
  themeColor?: 'blue' | 'rose' | 'gray'; // Ajout pour le styling
}

export default function TeamRow({
  team,
  winnerSide,
  teamIndex,
  showChampion,
  fontSize,
  themeColor
}: Props) {
  const isWinner = winnerSide !== undefined && winnerSide === teamIndex;
  const isLoser = winnerSide !== undefined && winnerSide !== teamIndex;

  return (
    <div className="flex items-center gap-3 w-full min-w-0">
      {/* Petite barre de couleur optionnelle pour identifier l'équipe */}
      {themeColor && (
        <div className={cn(
          "w-1 h-8 rounded-full shrink-0",
          themeColor === 'blue' && "bg-blue-500",
          themeColor === 'rose' && "bg-rose-500",
          themeColor === 'gray' && "bg-muted-foreground/30"
        )} />
      )}

      <div className="flex flex-col min-w-0 overflow-hidden">
        <div className={cn(
            "flex flex-col truncate leading-tight",
            fontSize ? fontSize : 'text-sm',
            isWinner ? 'font-bold text-foreground' : 'font-medium',
            isLoser ? 'text-muted-foreground' : 'text-foreground'
          )}>
          {team?.player1Name && (
            <span className="truncate">{team.player1Name}</span>
          )}

          {team?.player2Name && (
            <span className="truncate">{team.player2Name}</span>
          )}
        </div>

        {/* Affichage de la tête de série ou du trophée */}
        {(team?.displaySeed || showChampion) && (
          <div className="flex items-center gap-2 mt-0.5">
            {team?.displaySeed && (
              <span className="text-[10px] uppercase tracking-wider font-semibold text-muted-foreground bg-muted px-1.5 py-0.5 rounded-sm">
                TS {team.displaySeed}
              </span>
            )}
            {showChampion && (
              <span title="Vainqueur" className="text-yellow-500 animate-in fade-in zoom-in">
                <Trophy className="h-4 w-4 fill-current" />
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}