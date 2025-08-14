'use client';

import React from 'react';
import TournamentOverviewTab from './TournamentOverviewTab';
import { fetchTournament } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import { Loader2 } from 'lucide-react';

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
      <div className="flex items-center justify-center py-8 text-muted-foreground">
        <Loader2 className="h-6 w-6 animate-spin mr-2" />
        //Chargement...
      </div>
    );
  }

  return <TournamentOverviewTab tournament={tournament} />;
}