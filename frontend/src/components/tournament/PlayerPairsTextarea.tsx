'use client';

import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { toast } from 'react-toastify';

interface Props {
  tournamentId: number;
  onPairsChange?: (pairs: PlayerPair[]) => void; // OK dans interface
}

export default function PlayerPairsTextarea({ tournamentId, onPairsChange }: Props) {
  const [text, setText] = useState('');

  const fetchUpdatedPairs = async () => {
    try {
      const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/pairs`);
      if (response.ok) {
        const data: PlayerPair[] = await response.json();
        setText(data.map(pair => `${pair.player1.name},${pair.player2.name}`).join('\n'));
        if (onPairsChange) onPairsChange(data); // <-- maintenant ça fonctionne bien
      } else {
        toast.error("Impossible de récupérer les joueurs.");
      }
    } catch {
      toast.error("Erreur réseau lors du chargement des joueurs.");
    }
  };

  useEffect(() => {
    fetchUpdatedPairs();
  }, [tournamentId]);

  const handleClear = () => setText('');

  const handleSave = async () => {
    const lines = text
      .split('\n')
      .map(line => line.trim())
      .filter(line => line !== '');

    const pairs: PlayerPair[] = lines.map((line, index) => {
      const [p1, p2] = line.split(',').map(s => s.trim());
      return { player1: p1, player2: p2, seed: index + 1 };
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
      toast.error('Erreur réseau. Veuillez réessayer : ' + error);
    }
  };

  return (
    <div className="bg-white">
      <textarea
        className="w-full h-60 p-2 border border-gray-300 rounded resize-none font-mono overflow-x-auto whitespace-pre"
        wrap="off"
        value={text}
        onChange={e => setText(e.target.value)}
        placeholder={`Ghali Berrada,Selim Mekouar\nRedouane Bali,Ali Khobzaoui`}
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