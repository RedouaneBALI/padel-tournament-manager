'use client';

import React from 'react';
import TournamentOverviewTab from './TournamentOverviewTab';
import { fetchTournament } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

export default function TournamentOverviewPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = React.use(params);
  const [tournament, setTournament] = React.useState<Tournament | null>(null);

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

  if (!tournament) {
    return (
      <CenteredLoader />
    );
  }

  return <TournamentOverviewTab tournament={tournament} />;
}