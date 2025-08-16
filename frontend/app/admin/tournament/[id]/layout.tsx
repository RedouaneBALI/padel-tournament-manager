// app/admin/tournament/[id]/layout.tsx
'use client';

import React, { useEffect, useState, use } from 'react';
import { ToastContainer } from 'react-toastify';
import AdminTournamentHeader from '@/src/components/admin/AdminTournamentHeader';
import type { Tournament } from '@/src/types/tournament';
import 'react-toastify/dist/ReactToastify.css';
import { fetchTournamentAdmin } from '@/src/api/tournamentApi';
import { useRouter, usePathname } from 'next/navigation';
import { FiMoreHorizontal, FiUsers, FiSettings, FiList } from 'react-icons/fi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { LuSwords } from 'react-icons/lu';
import { TbTournament } from 'react-icons/tb';

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
  const items = [
    {
      href: `/admin/tournament/${id}/players`,
      label: 'Joueurs',
      Icon: FiUsers,
    },
    {
      href: `/admin/tournament/${id}/rounds/config`,
      label: 'Formats',
      Icon: FiSettings,
      isActive: (p) => p.includes(`/admin/tournament/${id}/rounds/config`),
    },
    {
      href: `/admin/tournament/${id}/games`,
      label: 'Matchs',
      Icon: LuSwords,
    },
    {
      href: `/admin/tournament/${id}/bracket`,
      label: 'Tableau',
      Icon: TbTournament,
    },
    // ➕ bouton \"Plus\" pour ouvrir le sous-menu du BottomNav
    {
      href: '#more',
      label: 'Plus',
      Icon: FiMoreHorizontal,
    },
  ];

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
      <div className="mt-6 mb-15">{children}</div>
      <BottomNav items={items} pathname={pathname} />
      <ToastContainer />
    </div>
  );
}