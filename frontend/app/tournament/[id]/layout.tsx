// app/tournament/[id]/layout.tsx
'use client';

import React, { useEffect, useState } from 'react';
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
import { TbTournament } from 'react-icons/tb';
import BottomNav from '@/src/components/ui/BottomNav';

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

  useEffect(() => {
    fetchTournament(id).then(setTournament);
  }, [id]);

  if (!tournament) {
    return <CenteredLoader />;
  }

  const items = [
    { href: `/tournament/${id}`, label: 'Aperçu', Icon: Home },
    { href: `/tournament/${id}/players`, label: 'Joueurs', Icon: Users },
    { href: `/tournament/${id}/games`, label: 'Matchs', Icon: LuSwords },
    { href: `/tournament/${id}/bracket`, label: 'Tableau', Icon: TbTournament },
  ];

  return (
    <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
      {/* Header simple avec nom du tournoi */}
      <header className="pt-4 pb-2">
        <div className="flex items-center gap-3">
          <div className="w-1 h-10 bg-primary rounded"></div>
           <span className="text-2xl font-bold tracking-tight text-primary">
              {tournament.name}
           </span>
        </div>
      </header>

      {/* Contenu avec padding bas pour ne pas passer sous la bottom bar */}
      <main className="pb-20">{children}</main>

      {/* Bottom navigation – remplace les onglets du haut */}
      <nav className="fixed bottom-0 inset-x-0 border-t border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 z-50">
        <div className="max-w-screen-2xl mx-auto px-2 sm:px-4">
          <BottomNav items={items} pathname={pathname} />
        </div>
      </nav>

      <ToastContainer />
    </div>
  );
}