'use client';

import React, { useEffect, useState, use } from 'react';
import { ToastContainer } from 'react-toastify';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import type { Tournament } from '@/src/types/tournament';
import 'react-toastify/dist/ReactToastify.css';
import { fetchTournamentAdmin } from '@/src/api/tournamentApi';
import { useRouter } from 'next/navigation';

import CenteredLoader from '@/src/components/ui/CenteredLoader';

export default function AdminTournamentLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    let mounted = true;
    async function loadTournament() {
      try {
        const data = await fetchTournamentAdmin(id);
        if (!mounted) return;
        if (data && data.isEditable === false) {
          router.replace('/403');
          return;
        }
        setTournament(data);
      } catch (e: any) {
        if (e?.message === 'FORBIDDEN') {
          router.replace('/403');
          return;
        }
        if (e?.message === 'UNAUTHORIZED') {
          router.replace('/');
          return;
        }
        // fallback erreur générique
        router.replace('/500');
      } finally {
        if (mounted) setLoading(false);
      }
    }
    loadTournament();
    return () => { mounted = false; };
  }, [id, router]);

  if (loading) {
    return (
      <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
<CenteredLoader />
        <ToastContainer />
      </div>
    );
  }

  if (!tournament) {
    return null;
  }

  return (
    <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
      <AdminTournamentHeader tournament={tournament} tournamentId={id} />
      <div className="mt-6">{children}</div>
      <ToastContainer />
    </div>
  );
}