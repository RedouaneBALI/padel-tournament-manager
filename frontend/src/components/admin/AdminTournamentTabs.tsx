// src/components/admin.AdminTournamentTabs
'use client';

import { useState } from 'react';
import { Users, Settings } from 'lucide-react';
import { LuSwords } from 'react-icons/lu';
import { TbTournament, TbTrophy } from 'react-icons/tb';
import { FiMoreHorizontal, FiPlusCircle, FiMail } from 'react-icons/fi';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import Link from 'next/link';
import type { IconType } from 'react-icons';

interface Props {
  tournamentId: string;
  pathname: string;
}

export default function AdminTournamentTabs({ tournamentId, pathname }: Props) {
  const base = `/admin/tournament/${tournamentId}`;

  const [isMoreOpen, setIsMoreOpen] = useState(false);

  const moreItems: BottomNavItem[] = [
    {
      href: `${base}/players`,
      label: 'Joueurs',
      Icon: Users,
      isActive: (p) => p.endsWith('/players'),
    },
    {
      href: `${base}/rounds/config`,
      label: 'Format',
      Icon: Settings,
      isActive: (p) => p.includes('/rounds/config'),
    },
    {
      href: `${base}/games`,
      label: 'Matchs',
      Icon: LuSwords,
      isActive: (p) => p.includes('/games'),
    },
    {
      href: `${base}/bracket`,
      label: 'Tableau',
      Icon: TbTournament,
      isActive: (p) => p.includes('/bracket'),
    },
    {
      href: '#more',
      label: 'Plus',
      Icon: FiMoreHorizontal,
      isActive: () => false,
    },
  ];

  return (
    <>
      {isMoreOpen && (
        <>
          <div
            className="fixed inset-0 bg-black/50 z-[60]"
            onClick={() => setIsMoreOpen(false)}
            aria-hidden
          />
          <div className="fixed inset-x-0 bottom-0 z-[70] bg-background rounded-t-2xl border-t border-border shadow-2xl">
            <div className="max-w-screen-sm mx-auto p-4">
              <div className="h-1.5 w-10 bg-muted-foreground/40 rounded-full mx-auto mb-4" />
              <nav className="flex flex-col">
                {moreItems.map(({ href, label, Icon }) => (
                  <Link
                    key={href}
                    href={href}
                    className="flex items-center gap-3 px-4 py-3 hover:bg-accent/80"
                  >
                  </Link>
                ))}
              </nav>
              <div className="pb-6" />
            </div>
          </div>
        </>
      )}
    </>
  );
}