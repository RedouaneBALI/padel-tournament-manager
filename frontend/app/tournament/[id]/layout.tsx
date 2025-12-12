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
import { ExportProvider } from '@/src/contexts/ExportContext';
import type { IconType } from 'react-icons';
import TournamentHeader from '@/src/components/ui/PageHeader';

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
  const moreItems = [
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
    {
      href: '#more',
      label: 'Plus',
      Icon: FiMoreHorizontal
    },

  ];

  useEffect(() => {
    fetchTournament(id).then(setTournament);
  }, [id]);

  const handleMoreClick = useCallback(() => {
    setIsMoreOpen(prev => !prev);
  }, []);

  const handleCloseMore = useCallback(() => {
    setIsMoreOpen(false);
  }, []);

  // Afficher le bouton retour uniquement sur les pages de détail de match (2 niveaux)
  // Ex: /tournament/4/games/81
  const isGameDetail = /\/tournament\/[^/]+\/games\/[^/]+$/.test(pathname);

  if (!tournament) {
    return <CenteredLoader />;
  }

  return (
    <ExportProvider>
      <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
        <header className="pt-4 pb-2">
          <div className="flex items-center gap-2">
            {isGameDetail && <TournamentHeader showBackButton title={tournament.name} />}
            {!isGameDetail && <TournamentHeader title={tournament.name} />}
          </div>
        </header>

        {/* Contenu avec padding bas pour ne pas passer sous la bottom bar */}
        <main className="mb-15">{children}</main>

        {/* Bottom navigation – remplace les onglets du haut */}
        <nav className="fixed bottom-0 inset-x-0 border-t border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 z-[80]">
          <div className="max-w-screen-2xl mx-auto px-2 sm:px-4">
            <BottomNav items={moreItems} pathname={pathname} onMoreClick={handleMoreClick} isMoreOpen={isMoreOpen} />
          </div>
        </nav>

        <ToastContainer />
      </div>
    </ExportProvider>
  );
}