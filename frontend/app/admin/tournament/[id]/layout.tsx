// app/admin/tournament/[id]/layout.tsx
'use client';

import React, { use, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { ToastContainer } from 'react-toastify';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import { ExportProvider } from '@/src/contexts/ExportContext';
import 'react-toastify/dist/ReactToastify.css';
import BottomNav from '@/src/components/ui/BottomNav';
import { getAdminTournamentItems } from '@/src/components/ui/bottomNavPresets';
import { fetchTournamentAdmin } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
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
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname() ?? '';
  const items = getAdminTournamentItems(id);

  useEffect(() => {
    let mounted = true;

    async function loadTournament() {
      try {
        const data = await fetchTournamentAdmin(id);
        if (!mounted) return;

        // Backend now returns isEditable correctly
        if (data.isEditable !== true) {
          const subPath = pathname.replace(`/admin/tournament/${id}`, '');
          const search = window.location.search;
          router.replace(`/tournament/${id}${subPath}${search}`);
          return;
        }

        setTournament(data);
      } catch (e: any) {
        if (!mounted) return;

        if (e?.message === 'FORBIDDEN' || e?.message === 'UNAUTHORIZED') {
          const subPath = pathname.replace(`/admin/tournament/${id}`, '');
          const search = window.location.search;
          router.replace(`/tournament/${id}${subPath}${search}`);
          return;
        }

        console.error("Failed to load tournament:", e);
      } finally {
        if (mounted) {
          setIsLoading(false);
        }
      }
    }

    loadTournament();
    return () => { mounted = false; };
  }, [id, router, pathname]);

  if (isLoading || !tournament) {
    return (
      <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
        <CenteredLoader />
        <ToastContainer />
      </div>
    );
  }

  return (
    <ExportProvider>
      <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
        <AdminTournamentHeader
          tournament={tournament}
          tournamentId={id}
        />
        <div className="mb-15">{children}</div>
        <BottomNav items={items} pathname={pathname} />
        <ToastContainer />
      </div>
    </ExportProvider>
  );
}

