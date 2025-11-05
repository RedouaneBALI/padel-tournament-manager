'use client';

import { fetchPairs, fetchTournament } from '@/src/api/tournamentApi';
import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Tournament } from '@/src/types/tournament';
import React from 'react';
import PlayerPairsList from '@/src/components/tournament/players/PlayerPairsList';

export default function TournamentPlayersTab({ params }: {   params: Promise<{ id: string }>;}) {
  const { id } = React.use(params);
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);
  const [loading, setLoading] = useState(true);
  const [tournamentStarted, setTournamentStarted] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const [pairsData, tournamentData] = await Promise.all([
          fetchPairs(id, false, false),
          fetchTournament(id),
        ]);

        if (!cancelled) {
          setPlayerPairs(pairsData);

          // Vérifier si le tournoi a démarré
          const hasStarted = !!(tournamentData as Tournament).rounds?.some(round =>
            round.games?.some(game => game.score !== null)
          );
          setTournamentStarted(hasStarted);
        }
      } catch (error) {
        console.error('Erreur lors du chargement des joueurs : ' + error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  return (
    <PlayerPairsList pairs={playerPairs} loading={loading} tournamentId={id} tournamentStarted={tournamentStarted} />
  );
}