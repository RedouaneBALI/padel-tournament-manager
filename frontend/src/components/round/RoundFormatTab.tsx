'use client';

import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import { MatchFormat } from '@/src/types/matchFormat';
import { Round } from '@/src/types/round';
import RoundSelector from '@/src/components/round/RoundSelector';
import MatchFormatActions from '@/src/components/round/MatchFormatActions';
import { PlayerPair } from '@/src/types/playerPair';
import { fetchRounds, fetchMatchFormat, updateMatchFormat } from '@/src/api/tournamentApi';

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
      try {
        const data = await fetchRounds(tournamentId);
        setRounds(data);
      } catch {
        toast.error('Erreur lors du chargement des rounds.');
      }
    }

    loadRounds();
  }, [tournamentId]);

  useEffect(() => {
    async function loadFormat() {
      if (!currentStage) return;
      setIsLoading(true);
      try {
        const format = await fetchMatchFormat(tournamentId, currentStage);
        setMatchFormat(format);
      } catch {
        toast.error('Erreur lors du chargement du format.');
      } finally {
        setIsLoading(false);
      }
    }

    loadFormat();
  }, [currentStage, tournamentId]);

  const saveFormat = async (newFormat: MatchFormat) => {
    if (!currentStage) return;
    try {
      await updateMatchFormat(tournamentId, currentStage, newFormat);
      toast.success('Format enregistré');
    } catch {
      toast.error("Erreur lors de l'enregistrement.");
    }
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
          <div className="text-center text-sm text-gray-500 mt-4">
            Chargement du format...
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