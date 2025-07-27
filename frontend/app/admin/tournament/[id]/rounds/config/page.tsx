'use client';

import React, { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import { stageLabels } from '@/src/types/stage';
import { MatchFormat }  from '@/src/types/matchFormat';
import { Round } from '@/src/types/round';

export default function MatchFormatConfigPage({ params }: { params: Promise<{ id: string }> }) {
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
          <div className="flex items-center justify-center gap-4">
            <button
              onClick={handlePrevious}
              disabled={currentStageIndex === 0}
              className="px-2 py-1 rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-30"
            >
              ←
            </button>

            <select
              value={currentStage}
              onChange={(e) => {
                const index = rounds.findIndex((r) => r.stage === e.target.value);
                if (index !== -1) setCurrentStageIndex(index);
              }}
              className="text-center text-sm px-3 py-1 border border-gray-300 rounded"
            >
              {rounds.map((round) => (
                <option key={round.id} value={round.stage}>
                  {stageLabels[round.stage]}
                </option>
              ))}
            </select>

            <button
              onClick={handleNext}
              disabled={currentStageIndex === rounds.length - 1}
              className="px-2 py-1 rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-30"
            >
              →
            </button>
          </div>

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

                  <button
                    onClick={async () => {
                      if (!matchFormat) return;
                      setIsLoading(true);
                      try {
                        await Promise.all(
                          rounds.map((round) =>
                            fetch(
                              `http://localhost:8080/tournaments/${id}/rounds/${round.stage}/match-format`,
                              {
                                method: 'PUT',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify(matchFormat),
                              }
                            )
                          )
                        );
                        toast.success('Format appliqué à tous les rounds.');
                      } catch {
                        toast.error('Erreur lors de la mise à jour des rounds.');
                      } finally {
                        setIsLoading(false);
                      }
                    }}
                    disabled={isLoading}
                    className="mt-4 px-4 py-2 bg-gray-100 text-sm border border-gray-300 rounded hover:bg-gray-200 disabled:opacity-50"
                  >
                    Appliquer à tous les rounds
                  </button>
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