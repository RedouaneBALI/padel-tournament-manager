'use client';

import React from 'react';
import { Round } from '@/src/types/round';
import { MatchFormat } from '@/src/types/matchFormat';
import { toast } from 'react-toastify';
import { confirmAlert } from 'react-confirm-alert';
import { useRouter } from 'next/navigation';

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
    try {
      await Promise.all(
        rounds.map((round) =>
          fetch(
            `http://localhost:8080/tournaments/${tournamentId}/rounds/${round.stage}/match-format`,
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
  };

  const isFirstRoundEmpty = rounds[0]?.games.every(
    (game) => !game.teamA && !game.teamB
  );

  const handleDraw = () => {
    confirmAlert({
      title: 'Confirmer le tirage',
      message:
        'Êtes-vous sûr de vouloir générer le tirage ? Cette action créera tous les matchs du premier round.',
      buttons: [
        {
          label: 'Oui',
          onClick: async () => {
            try {
              const res = await fetch(`http://localhost:8080/tournaments/${tournamentId}/draw`, {
                method: 'POST',
              });

              if (!res.ok) {
                toast.error("Tirage déjà effectué.");
                return;
              }

              toast.success("Tirage généré !");
              router.push(`/admin/tournament/${tournamentId}/rounds/results`);
            } catch {
              toast.error("Erreur lors de la génération du tirage.");
            }
          },
        },
        {
          label: 'Annuler',
          onClick: () => {},
        },
      ],
    });
  };

  return (
    <div className="flex flex-col sm:flex-row gap-2 mt-4">
      <button
        onClick={handleApplyAll}
        disabled={isLoading}
        className="w-full sm:w-auto px-4 py-2 bg-white text-sm border border-gray-300 text-gray-800 rounded hover:bg-gray-100 disabled:opacity-50"
      >
        Appliquer à tous les rounds
      </button>

    {currentStageIndex === 0 && isFirstRoundEmpty && (
      <button
        onClick={handleDraw}
        className="w-full sm:w-auto px-4 py-2 bg-white text-sm border border-gray-300 text-gray-800 rounded hover:bg-gray-100"
      >
        Générer le tirage
      </button>
    )}
    </div>
  );
}