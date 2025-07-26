'use client';

import React, {use, useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import MatchFormatForm, { MatchFormat } from '@/src/components/round/MatchFormatForm';

export default function MatchFormatConfigPage({ params }: { params: { id: string } }) {
  const { id } = use(params);
  const [format, setFormat] = useState<MatchFormat>({
    numberOfSetsToWin: 2,
    pointsPerSet: 6,
    superTieBreakInFinalSet: true,
    advantage: false,
  });

  useEffect(() => {
    async function fetchFormat() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}/match-format`);
        if (!res.ok) return;
        const data = await res.json();
        setFormat(data);
      } catch {
        toast.error('Erreur lors du chargement du format de match.');
      }
    }
    fetchFormat();
  }, [id]);

  const saveFormat = async (newFormat: MatchFormat) => {
    try {
      const res = await fetch(`http://localhost:8080/tournaments/${id}/match-format`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newFormat),
      });
      if (!res.ok) throw new Error();
      toast.success('Format enregistré !');
    } catch {
      toast.error('Erreur lors de l’enregistrement du format.');
    }
  };

  const handleDraw = async () => {
    try {
      const res = await fetch(`http://localhost:8080/tournaments/${id}/draw`, { method: 'POST' });
      if (!res.ok) throw new Error();
      toast.success('Tirage généré avec succès !');
    } catch {
      toast.error('Erreur lors de la génération du tirage.');
    }
  };

  return (
    <>
      <MatchFormatForm format={format} onChange={f => { setFormat(f); saveFormat(f); }} />
      <button
        onClick={handleDraw}
        className="mt-4 px-4 py-2 bg-emerald-600 text-white rounded hover:bg-emerald-700"
      >
        Générer le tirage
      </button>
    </>
  );
}