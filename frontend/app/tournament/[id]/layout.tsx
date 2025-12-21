// app/tournament/[id]/layout.tsx
'use client';

import React, { useEffect, useState, useCallback, useMemo } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
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
import PageHeader from '@/src/components/ui/PageHeader';
import { TournamentContext } from '@/src/contexts/TournamentContext';

export default function TournamentLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = React.use(params);
  const pathname = usePathname() ?? '';
  const router = useRouter();
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [isMoreOpen, setIsMoreOpen] = useState(false);

  // Navigation items for bottom navigation
  const moreItems = useMemo(() => {
    const isSingleMatchTournament = tournament?.config?.mainDrawSize === 2 &&
      tournament.rounds?.length === 1 &&
      tournament.rounds[0]?.games?.length === 1;

    const gamesHref = isSingleMatchTournament
      ? `/tournament/${id}/games/${tournament.rounds[0].games[0].id}`
      : `/tournament/${id}/games`;

    return [
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
        href: gamesHref,
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
  }, [tournament, id]);

  useEffect(() => {
    fetchTournament(id)
      .then((data) => {
        if (!data) {
          window.location.href = '/404';
          return;
        }
        setTournament(data);
      })
      .catch((e) => {
        if (e?.message?.startsWith('HTTP_401') || e?.message?.startsWith('HTTP_404') || e?.message?.startsWith('HTTP_500')) {
          window.location.href = '/404';
        }
      });
  }, [id, router]);

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
      <TournamentContext.Provider value={{ name: tournament.name, club: tournament.club, level: tournament.level }}>
        <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
          <header className="pt-4 pb-2">
            <div className="flex items-center gap-2">
              {isGameDetail && <PageHeader showBackButton title={tournament.name} />}
              {!isGameDetail && <PageHeader title={tournament.name} />}
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
      </TournamentContext.Provider>
    </ExportProvider>
  );
}