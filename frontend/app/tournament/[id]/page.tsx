'use client';

import { useRouter } from 'next/navigation';
import { useState, useEffect } from 'react';
import React from 'react';
import { toast } from 'react-toastify';
import { ToastContainer } from 'react-toastify';

interface PlayerPairInput {
  player1: string;
  player2: string;
}

export default function TournamentPage({ params }: { params: { id: string } }) {
  const { id } = React.use(params);
  const baseUrl = typeof window !== 'undefined' ? window.location.origin : '';

  const [pairs, setPairs] = useState<PlayerPairInput[]>([{ player1: '', player2: '' }]);
  const [isSubmitting, setIsSubmitting] = useState(false);


  useEffect(() => {
      async function fetchPairs() {
        try {
          const response = await fetch(`http://localhost:8080/tournaments/${id}/pairs`);
          if (response.ok) {
            const data: PlayerPairInput[] = await response.json();
            if (data.length > 0) {
              setPairs(data);
              // Construire la chaîne multi-lignes pour textarea
              const text = data.map(pair => `${pair.player1},${pair.player2}`).join('\n');
              setBulkInput(text);
            }
          } else {
            toast.error('Impossible de récupérer les joueurs existants.');
          }
        } catch {
          toast.error('Erreur réseau lors de la récupération des joueurs.');
        }
      }

      fetchPairs();
    }, [id]);


  // Gestion du formulaire pour ajouter des paires
  function handlePairChange(index: number, field: keyof PlayerPairInput, value: string) {
    const newPairs = [...pairs];
    newPairs[index][field] = value;
    setPairs(newPairs);
  }

  // Ajouter une nouvelle paire vide
  function addPair() {
    setPairs([...pairs, { player1: '', player2: '' }]);
  }

  // Supprimer une paire
  function removePair(index: number) {
    setPairs(pairs.filter((_, i) => i !== index));
  }

  // Bulk input via textarea
  const [bulkInput, setBulkInput] = useState('');


  function parseBulkInput() {
    const lines = bulkInput.split('\n').filter(line => line.trim() !== '');
    const newPairs = lines.map(line => {
      const [player1, player2] = line.split(',').map(name => name.trim());
      return { player1: player1 || '', player2: player2 || '' };
    });
    setPairs(newPairs);
    addPairsToBackend(newPairs);
  }

  function clearBulkInput() {
    setBulkInput('');
  }

  async function addPairsToBackend(pairs: PlayerPairInput[]) {
    setIsSubmitting(true);

    try {
      const response = await fetch(`http://localhost:8080/tournaments/${id}/pairs`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(pairs),
      });

      if (response.ok) {
        toast.success('Joueurs ajoutés avec succès !');
      } else {
        toast.error('Erreur lors de l’ajout des joueurs.');
      }
    } catch {
      toast.error("Erreur réseau. Veuillez réessayer.");
    } finally {
      setIsSubmitting(false);
    }
  }

  // Copier le lien dans le presse-papiers
  async function copyLink() {
    if (baseUrl) {
      await navigator.clipboard.writeText(`${baseUrl}/tournament/${id}`);
      toast.success('Lien copié dans le presse-papiers !');
    }
  }

  // Envoi des paires au backend
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const response = await fetch(`http://localhost:8080/tournaments/${id}/playerPairs`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(playerPairs),
      });

      if (response.ok) {
        toast.success('Joueurs ajoutés avec succès !');
      } else {
        toast.error('Erreur lors de l’ajout des joueurs.');
      }
    } catch {
      toast.error("Erreur réseau. Veuillez réessayer.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">Tournoi #{id}</h1>

      <section className="mb-8">
        <p>Partagez ce lien avec les joueurs :</p>
        <div className="flex items-center gap-2 mt-2">
          <input
            readOnly
            value={`${baseUrl}/tournament/${id}`}
            className="flex-1 border px-3 py-2 rounded"
          />
          <button
            onClick={copyLink}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Copier
          </button>
        </div>
      </section>

      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-4">Lister les joueurs ci-dessous (par ordre de classement)</h2>
        <textarea
          value={bulkInput}
          onChange={(e) => setBulkInput(e.target.value)}
          placeholder={`Michel,Bob
Karl,Eric`}
          rows={6}
          className="w-full px-3 py-2 border rounded mb-2"
        />
        <div className="flex gap-2">
          <button
            onClick={parseBulkInput}
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Enregistrer les joueurs
          </button>
          <button
            onClick={clearBulkInput}
            className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
          >
            Reset
          </button>
        </div>
        <ToastContainer />
      </section>

    </div>
  );
}