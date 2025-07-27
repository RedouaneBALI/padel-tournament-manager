'use client';

import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import { stageLabels } from '@/src/types/stage';
import { MatchFormat } from '@/src/types/matchFormat';
import { Round } from '@/src/types/round';
import RoundSelector from '@/src/components/round/RoundSelector';
import MatchFormatActions from '@/src/components/round/MatchFormatActions';
import { PlayerPair } from '@/src/types/playerPair';

interface MatchFormatConfigPageProps {
  params: Promise<{ id: string }>;
}

export default function MatchFormatConfigPage({ params }: MatchFormatConfigPageProps) {
  const { id } = React.use(params);
  const [rounds, setRounds] = useState<Round[]>([]);
  const [currentStageIndex, setCurrentStageIndex] = useState<number>(0);
  const [matchFormat, setMatchFormat] = useState<MatchFormat | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const currentStage = rounds[currentStageIndex]?.stage;
  useEffect(() => {
    async function fetchRounds() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}/rounds`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        setRounds(data);
      } catch {
        toast.error('Erreur lors du chargement des rounds.');
      }
    }

    fetchRounds();
  }, [id]);

  useEffect(() => {
    async function fetchFormat() {
      if (!currentStage) return;
      setIsLoading(true);
      try {
        const res = await fetch(
          `http://localhost:8080/tournaments/${id}/rounds/${currentStage}/match-format`
        );
        if (!res.ok) throw new Error();
        const format = await res.json();
        setMatchFormat(format);
      } catch {
        toast.error('Erreur lors du chargement du format.');
      } finally {
        setIsLoading(false);
      }
    }

    fetchFormat();
  }, [currentStage, id]);

  const saveFormat = async (newFormat: MatchFormat) => {
    if (!currentStage) return;
    try {
      const res = await fetch(
        `http://localhost:8080/tournaments/${id}/rounds/${currentStage}/match-format`,
        {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newFormat),
        }
      );
      if (!res.ok) throw new Error();
      toast.success('Format enregistré');
    } catch {
      toast.error("Erreur lors de l'enregistrement.");
    }
  };

  const handleDraw = async () => {
    try {
      const res = await fetch(`http://localhost:8080/tournaments/${id}/draw`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error();
      toast.success('Tirage généré');
      const roundRes = await fetch(`http://localhost:8080/tournaments/${id}/rounds`);
      const data = await roundRes.json();
      setRounds(data);
      setCurrentStageIndex(0);
    } catch {
      toast.error('Erreur lors de la génération du tirage.');
    }
  };

  const handlePrevious = () => {
    setCurrentStageIndex((i) => Math.max(i - 1, 0));
  };

  const handleNext = () => {
    setCurrentStageIndex((i) => Math.min(i + 1, rounds.length - 1));
  };

  return (
    <div className="space-y-4">
      {rounds.length > 0 ? (
        <>
          <RoundSelector
            rounds={rounds}
            currentIndex={currentStageIndex}
            onChange={setCurrentStageIndex}
          />

          {isLoading ? (
            <div className="text-center text-sm text-gray-500 mt-4">Chargement du format...</div>
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
                  tournamentId={id}
                  rounds={rounds}
                  currentStageIndex={currentStageIndex}
                  matchFormat={matchFormat}
                  setRounds={setRounds}
                  setCurrentStageIndex={setCurrentStageIndex}
                  isLoading={isLoading}
                  setIsLoading={setIsLoading}
                />
              </>
            )
          )}
        </>
      ) : (
        <>
          <p className="text-sm text-gray-600">
            Aucun round encore généré. Cliquez sur le bouton ci-dessous pour générer un tirage.
          </p>
          <button
            onClick={handleDraw}
            className="mt-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Générer le tirage
          </button>
        </>
      )}
    </div>
  );
}