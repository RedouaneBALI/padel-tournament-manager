'use client';

import React from 'react';
import TournamentOverviewTab from './TournamentOverviewTab';

export default function TournamentOverviewPage({ params }: { params: Promise<{ id: string }> }) {
  const actualParams = React.use(params);
  const { id } = actualParams;

  const [tournament, setTournament] = React.useState(null);

  React.useEffect(() => {
    async function fetchTournament() {
      const res = await fetch(`http://localhost:8080/tournaments/${id}`);
      if (res.ok) {
        setTournament(await res.json());
      }
    }
    fetchTournament();
  }, [id]);

  if (!tournament) return <div>Chargement...</div>;

  return <TournamentOverviewTab tournament={tournament} />;
}