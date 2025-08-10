'use client';

import React, { useEffect, useState, use } from 'react';
import { ToastContainer } from 'react-toastify';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import type { Tournament } from '@/src/types/tournament';
import 'react-toastify/dist/ReactToastify.css';
import { fetchTournament } from '@/src/api/tournamentApi';

export default function AdminTournamentLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    async function loadTournament() {
    const data = await fetchTournament(id);
    setTournament(data);
    }

    loadTournament();
  }, [id]);

  return (
    <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
      <AdminTournamentHeader tournament={tournament} tournamentId={id} />
      <div className="mt-6">{children}</div>
      <ToastContainer />
    </div>
  );
}