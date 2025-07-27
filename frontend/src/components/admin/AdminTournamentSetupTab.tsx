'use client';

import React, { useEffect, useState } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import PlayerPairsTextarea from '@/src/components/tournament/PlayerPairsTextarea';
import { FileText } from 'lucide-react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import { MatchFormat } from '@/src/types/matchFormat';
import { Round } from '@/src/types/round';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';

interface Props {
  tournamentId: string;
}

export default function AdminTournamentSetupTab({ tournamentId }: Props) {
  const baseUrl = typeof window !== 'undefined' ? window.location.origin : '';
  const [pairs, setPairs] = useState<PlayerPair[]>([]);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [rounds, setRounds] = useState<Round[]>([]);
  const [matchFormat, setMatchFormat] = useState<MatchFormat>({
    numberOfSetsToWin: 2,
    pointsPerSet: 6,
    superTieBreakInFinalSet: true,
    advantage: false,
  });

  useEffect(() => {
    async function fetchTournament() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}`);
        if (response.ok) {
          const data: Tournament = await response.json();
          setTournament(data);
        } else {
          toast.error('Impossible de récupérer les infos du tournoi.');
        }
      } catch {
        toast.error('Erreur réseau lors de la récupération du tournoi.');
      }
    }
    fetchTournament();
  }, [tournamentId]);

  useEffect(() => {
    async function fetchPairs() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/pairs`);
        if (response.ok) {
          const data: PlayerPair[] = await response.json();
          setPairs(data);
        } else {
          toast.error('Impossible de récupérer les joueurs existants.');
        }
      } catch {
        toast.error('Erreur réseau lors de la récupération des joueurs.');
      }
    }
    fetchPairs();
  }, [tournamentId]);

  async function copyLink() {
    if (baseUrl) {
      await navigator.clipboard.writeText(`${baseUrl}/tournament/${tournamentId}`);
      toast.success('Lien copié dans le presse-papiers !');
    }
  }

  const handleDraw = async () => {
    try {
      const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/draw`, {
        method: 'POST',
      });

      if (!response.ok) {
        toast.error('Erreur lors de la génération du tirage.');
        return;
      }

      const newRound: Round = await response.json();
      setRounds(prev => [...prev, newRound]);
      toast.success('Tirage généré avec succès !');
    } catch (error) {
      console.error(error);
      toast.error('Une erreur est survenue.');
    }
  };

  return (
    <div className="container mx-auto max-w-3xl">
      <div className="bg-card shadow-sm p-6 space-y-6">
        <div className="flex items-center gap-2 border-b border-border pb-3">
          <FileText className="h-5 w-5 text-muted-foreground" />
          <h1 className="text-xl font-semibold text-foreground">
            {pairs.length} joueurs
          </h1>
        </div>

        <section>
          <h2 className="mb-3 text-base font-semibold text-foreground">
            Lister les joueurs ci-dessous (par ordre de classement)
          </h2>
          <PlayerPairsTextarea
            onPairsChange={setPairs}
            tournamentId={Number(tournamentId)}
          />
        </section>

      </div>

      <ToastContainer />
    </div>
  );
}