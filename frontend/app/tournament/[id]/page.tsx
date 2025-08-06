'use client';

import React from 'react';
import TournamentOverviewTab from './TournamentOverviewTab';
import { fetchTournament } from '@/src/api/tournamentApi';

export default function TournamentOverviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = React.use(params);

  const [tournament, setTournament] = React.useState(null);

  React.useEffect(() => {
    async function loadTournament() {
      try {
        const tournament = await fetchTournament(id);
        setTournament(tournament);
      } catch (error) {
        console.error(error);
      }
    }
    loadTournament();
  }, [id]);

  if (!tournament) return <div>Chargement...</div>;

  return <TournamentOverviewTab tournament={tournament} />;
}