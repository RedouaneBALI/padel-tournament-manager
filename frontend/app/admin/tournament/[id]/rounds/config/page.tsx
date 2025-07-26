'use client';

import React, { use, useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import MatchFormatForm, { MatchFormat } from '@/src/components/round/MatchFormatForm';

export default function MatchFormatConfigPage({ params }: { params: { id: string } }) {
  const { id } = use(params)
  const [roundId, setRoundId] = useState<number | null>(null);
  const [format, setFormat] = useState<MatchFormat>({
    numberOfSetsToWin: 2,
    pointsPerSet: 6,
    superTieBreakInFinalSet: true,
    advantage: false,
  });

  useEffect(() => {
    async function fetchFirstRound() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}/rounds`);
        if (!res.ok) throw new Error();
        const rounds = await res.json();

        if (rounds.length > 0) {
          const firstRound = rounds[0];
          setRoundId(firstRound.id);

          // Ensuite, fetch le match format du round
          const formatRes = await fetch(`http://localhost:8080/tournaments/${id}/rounds/${firstRound.id}/match-format`);
          if (!formatRes.ok) throw new Error();
          const matchFormat = await formatRes.json();
          setFormat(matchFormat);
        }
      } catch {
        toast.error('Erreur lors du chargement du format ou des rounds.');
      }
    }

    fetchFirstRound();
  }, [id]);

  const saveFormat = async (newFormat: MatchFormat) => {
    if (!roundId) {
      toast.error('Aucun round existant pour enregistrer le format.');
      return;
    }

    try {
      const res = await fetch(
        `http://localhost:8080/tournaments/${id}/rounds/${roundId}/match-format`,
        {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(newFormat),
        }
      );
      if (!res.ok) throw new Error();
      toast.success('Format enregistré !');
    } catch {
      toast.error("Erreur lors de l'enregistrement du format.");
    }
  };

  const handleDraw = async () => {
    try {
      const res = await fetch(`http://localhost:8080/tournaments/${id}/draw`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error();
      toast.success('Tirage généré avec succès !');

      // Recharge les rounds (et le match format du premier)
      const roundRes = await fetch(`http://localhost:8080/tournaments/${id}/rounds`);
      if (!roundRes.ok) throw new Error();
      const rounds = await roundRes.json();

      if (rounds.length > 0) {
        const first = rounds[0];
        setRoundId(first.id);
        const formatRes = await fetch(`http://localhost:8080/tournaments/${id}/rounds/${first.id}/match-format`);
        const matchFormat = await formatRes.json();
        setFormat(matchFormat);
      }
    } catch {
      toast.error('Erreur lors de la génération du tirage.');
    }
  };

  return (
    <>
      {roundId ? (
        <>
          <MatchFormatForm
            format={format}
            onChange={(f) => {
              setFormat(f);
              saveFormat(f);
            }}
          />
          <button
            onClick={handleDraw}
            className="mt-4 px-4 py-2 bg-emerald-600 text-white rounded hover:bg-emerald-700"
          >
            Générer le tirage
          </button>
        </>
      ) : (
        <>
          <p className="mb-4 text-sm text-gray-600">
            Aucun round encore généré. Cliquez sur le bouton ci-dessous pour générer un tirage.
          </p>
          <button
            onClick={handleDraw}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Générer le tirage
          </button>
        </>
      )}
    </>
  );
}