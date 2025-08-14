'use client';

import React, { useEffect, useState } from 'react';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import { MatchFormat } from '@/src/types/matchFormat';
import { Round } from '@/src/types/round';
import RoundSelector from '@/src/components/round/RoundSelector';
import MatchFormatActions from '@/src/components/round/MatchFormatActions';
import { PlayerPair } from '@/src/types/playerPair';
import { fetchRounds, fetchMatchFormat, updateMatchFormat } from '@/src/api/tournamentApi';
import { Loader2 } from 'lucide-react';

interface RoundFormatTabProps {
  tournamentId: string;
  pairs: PlayerPair[];
}

export default function RoundFormatTab({ tournamentId, pairs }: RoundFormatTabProps) {
  const [rounds, setRounds] = useState<Round[]>([]);
  const [currentStageIndex, setCurrentStageIndex] = useState<number>(0);
  const [matchFormat, setMatchFormat] = useState<MatchFormat | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const currentStage = rounds[currentStageIndex]?.stage;

  useEffect(() => {
    async function loadRounds() {
    const data = await fetchRounds(tournamentId);
    setRounds(data);

    }

    loadRounds();
  }, [tournamentId]);

  useEffect(() => {
    async function loadFormat() {
      if (!currentStage) return;
      setIsLoading(true);
      const format = await fetchMatchFormat(tournamentId, currentStage);
      setMatchFormat(format);
      setIsLoading(false);
    }

    loadFormat();
  }, [currentStage, tournamentId]);

  const saveFormat = async (newFormat: MatchFormat) => {
    if (!currentStage) return;
    await updateMatchFormat(tournamentId, currentStage, newFormat);
  };

  return (
    <div className="space-y-4">
      <>
        <RoundSelector
          rounds={rounds}
          currentIndex={currentStageIndex}
          onChange={setCurrentStageIndex}
        />

        {isLoading ? (
          <div className="flex items-center justify-center py-8 text-muted-foreground">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        ) : (
          matchFormat && (
            <>
              <MatchFormatForm
                format={matchFormat}
                onChange={(newFormat) => {
                  setMatchFormat(newFormat);
                  saveFormat(newFormat);
                }}
              />
              <MatchFormatActions
                tournamentId={tournamentId}
                rounds={rounds}
                currentStageIndex={currentStageIndex}
                matchFormat={matchFormat}
                isLoading={isLoading}
                setIsLoading={setIsLoading}
              />
            </>
          )
        )}
      </>
    </div>
  );
}