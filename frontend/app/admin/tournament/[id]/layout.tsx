// app/admin/tournament/[id]/layout.tsx
'use client';

import React, { useEffect, useState, use } from 'react';
import { ToastContainer } from 'react-toastify';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import { ExportProvider } from '@/src/contexts/ExportContext';
import type { Tournament } from '@/src/types/tournament';
import 'react-toastify/dist/ReactToastify.css';
import { fetchTournamentAdmin } from '@/src/api/tournamentApi';
import { useRouter, usePathname } from 'next/navigation';
import { FiList } from 'react-icons/fi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import BottomNav from '@/src/components/ui/BottomNav';
import { getAdminTournamentItems } from '@/src/components/ui/bottomNavPresets';

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

  const pathname = usePathname() ?? '';
  const items = getAdminTournamentItems(id);

  useEffect(() => {
    let mounted = true;
    async function loadTournament() {
      try {
        const data = await fetchTournamentAdmin(id);
        if (!mounted) return;
        if (data && data.isEditable === false) {
          // Redirect to read-only public version instead of 403
          // Preserve sub-path if possible (e.g., /admin/tournament/12/bracket → /tournament/12/bracket)
          const currentPath = pathname ?? '';
          const subPath = currentPath.replace(`/admin/tournament/${id}`, '');
          router.replace(`/tournament/${id}${subPath}`);
          return;
        }
        setTournament(data);
      } catch (e: any) {
        if (e?.message === 'FORBIDDEN') {
          // Redirect to read-only version for forbidden access
          const currentPath = pathname ?? '';
          const subPath = currentPath.replace(`/admin/tournament/${id}`, '');
          router.replace(`/tournament/${id}${subPath}`);
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
  }, [id, router, pathname]);

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
    <ExportProvider>
      <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
        <AdminTournamentHeader tournament={tournament} tournamentId={id} />
        <div className="mb-15">{children}</div>
        <BottomNav items={items} pathname={pathname} />
        <ToastContainer />
      </div>
    </ExportProvider>
  );
}