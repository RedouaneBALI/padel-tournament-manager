'use client';

import React, { useEffect, useState, use } from 'react';
import { ToastContainer, toast } from 'react-toastify';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import type { Tournament } from '@/src/types/tournament';

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
    async function fetchTournament() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        setTournament(data);
      } catch (err) {
        toast.error('Erreur lors du chargement du tournoi : ' + err);
      }
    }

    fetchTournament();
  }, [id]);

  return (
    <div className="max-w-4xl mx-auto p-6">
      <AdminTournamentHeader tournament={tournament} tournamentId={id} />
      <div className="mt-6">{children}</div>
      <ToastContainer />
    </div>
  );
}