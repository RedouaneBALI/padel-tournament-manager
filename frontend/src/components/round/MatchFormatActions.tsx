'use client';

import React from 'react';
import { Round } from '@/src/types/round';
import { MatchFormat } from '@/src/types/matchFormat';
import { toast } from 'react-toastify';
import { confirmAlert } from 'react-confirm-alert';
import { useRouter } from 'next/navigation';
import { updateMatchFormat, generateDraw } from '@/src/api/tournamentApi';

interface Props {
  tournamentId: string;
  rounds: Round[];
  matchFormat: MatchFormat | null;
  currentStageIndex: number;
  isLoading: boolean;
  setIsLoading: (value: boolean) => void;
}

export default function MatchFormatActions({
  tournamentId,
  rounds,
  matchFormat,
  currentStageIndex,
  isLoading,
  setIsLoading,
}: Props) {
  const router = useRouter();

  const handleApplyAll = async () => {
    if (!matchFormat) return;
    setIsLoading(true);
      await Promise.all(
        rounds.map((round) =>
          updateMatchFormat(tournamentId, round.stage, matchFormat)
        )
      );
      setIsLoading(false);
  };

  const isFirstRoundEmpty = rounds[0]?.games.every(
    (game) => !game.teamA && !game.teamB
  );

  return (
    <div className="flex flex-col sm:flex-row gap-2 mt-4 sm:w-auto justify-center items-center">
      <button
        onClick={handleApplyAll}
        disabled={isLoading}
        className="w-full px-4 py-2 bg-card text-sm border border-border text-foreground rounded hover:bg-background disabled:opacity-50 max-w-[400px]"
      >
        Appliquer à tous les rounds
      </button>

    </div>
  );
}