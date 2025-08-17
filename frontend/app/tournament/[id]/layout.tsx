// app/tournament/[id]/layout.tsx
'use client';

import React, { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import type { Tournament } from '@/src/types/tournament';
import { fetchTournament } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import type { ReactNode } from 'react';
import { Home, Users } from 'lucide-react';
import { LuSwords } from 'react-icons/lu';
import { TbTournament, TbTrophy } from 'react-icons/tb';
import { FiMoreHorizontal, FiPlusCircle, FiMail } from 'react-icons/fi';
import BottomNav from '@/src/components/ui/BottomNav';
import type { IconType } from 'react-icons';
import PageHeader from '@/src/components/ui/PageHeader';

export default function TournamentLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = React.use(params);
  const pathname = usePathname() ?? '';
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [isMoreOpen, setIsMoreOpen] = useState(false);

  // Navigation items for bottom navigation
  const items = [
    {
      href: `/tournament/${id}`,
      label: 'Home',
      Icon: Home,
    },
    {
      href: `/tournament/${id}/players`,
      label: 'Joueurs',
      Icon: Users,
    },
    {
      href: `/tournament/${id}/games`,
      label: 'Matchs',
      Icon: LuSwords,
    },
    {
      href: `/tournament/${id}/bracket`,
      label: 'Tableau',
      Icon: TbTournament,
    },
  ];

  useEffect(() => {
    fetchTournament(id).then(setTournament);
  }, [id]);

  const handleMoreClick = useCallback(() => {
    setIsMoreOpen(true);
  }, []);

  const handleCloseMore = useCallback(() => {
    setIsMoreOpen(false);
  }, []);

  if (!tournament) {
    return <CenteredLoader />;
  }

  return (
    <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
      <header className="pt-4 pb-2">
        <PageHeader title={tournament.name} />
      </header>

      {/* Contenu avec padding bas pour ne pas passer sous la bottom bar */}
      <main className="mb-15">{children}</main>

      {/* Bottom navigation – remplace les onglets du haut */}
      <nav className="fixed bottom-0 inset-x-0 border-t border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 z-50">
        <div className="max-w-screen-2xl mx-auto px-2 sm:px-4">
          <BottomNav items={items} pathname={pathname} />
        </div>
      </nav>

      <ToastContainer />

      {isMoreOpen && (
        <>
          <div
            className="fixed inset-0 bg-black/50 z-[60]"
            onClick={handleCloseMore}
            aria-hidden
          />
          <div className="fixed inset-x-0 bottom-0 z-[70] bg-background rounded-t-2xl border-t border-border shadow-2xl">
            <div className="max-w-screen-sm mx-auto p-4">
              <div className="h-1.5 w-10 bg-muted-foreground/40 rounded-full mx-auto mb-4" />
              <nav className="flex flex-col divide-y divide-border">
                {moreItems.map(({ href, label, Icon }) => (
                  <Link
                    key={href}
                    href={href}
                    onClick={handleCloseMore}
                    className="flex items-center gap-3 px-4 py-3 hover:bg-accent/80"
                  >
                    <Icon size={20} className="text-foreground" aria-hidden />
                    <span>{label}</span>
                  </Link>
                ))}
              </nav>
            </div>
          </div>
        </>
      )}
    </div>
  );
}