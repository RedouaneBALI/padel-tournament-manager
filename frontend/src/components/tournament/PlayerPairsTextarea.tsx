'use client';

import { useEffect, useState } from 'react';
import { SimplePlayerPair } from '@/app/types/PlayerPair';
import { toast } from 'react-toastify';

interface Props {
  tournamentId: number;
  defaultPairs: SimplePlayerPair[];
}

export default function PlayerPairsTextarea({ tournamentId, defaultPairs }: Props) {
  const [text, setText] = useState('');

  const fetchUpdatedPairs = async () => {
    try {
      const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/pairs`);
      if (response.ok) {
        const data: SimplePlayerPair[] = await response.json();
        setText(data.map(pair => `${pair.player1},${pair.player2}`).join('\n'));
      } else {
        toast.error("Impossible de récupérer les joueurs.");
      }
    } catch {
      toast.error("Erreur réseau lors du chargement des joueurs.");
    }
  };

  useEffect(() => {
    fetchUpdatedPairs(); // ← appel initial au montage
  }, [tournamentId]);

  const handleClear = () => setText('');


  const handleSave = async () => {
    const lines = text
      .split('\n')
      .map(line => line.trim())
      .filter(line => line !== '');

    const pairs: SimplePlayerPair[] = lines.map(line => {
      const [p1, p2] = line.split(',').map(s => s.trim());
      return { player1: p1, player2: p2 };
    });



    try {
      const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/pairs`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(pairs),
      });

      if (!response.ok) {
        toast.error("Erreur lors de l'enregistrement des joueurs.");
        return;
      }

      toast.success('Joueurs ajoutés avec succès !');
      await fetchUpdatedPairs();
    } catch (error) {
      toast.error('Erreur réseau. Veuillez réessayer.');
    }
  };



  return (
    <div className="bg-white p-4 rounded-md shadow">
      <textarea
        className="w-full h-60 p-2 border border-gray-300 rounded resize-none font-mono"
        value={text}
        onChange={e => setText(e.target.value)}
        placeholder="Michel - Bob\nRedouane - Ali"
      />

      <div className="flex justify-end gap-4 mt-4">
        <button
          onClick={handleClear}
          className="px-4 py-2 border border-gray-300 text-gray-700 rounded hover:bg-gray-100"
        >
          Effacer
        </button>
        <button
          onClick={handleSave}
          className="px-4 py-2 bg-[#1b2d5e] text-white rounded hover:bg-blue-900"
        >
          Enregistrer les joueurs
        </button>
      </div>
    </div>
  );
}