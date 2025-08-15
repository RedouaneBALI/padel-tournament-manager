// src/components/admin.AdminTournamentTabs
'use client';

import { Users, Settings } from 'lucide-react';
import { LuSwords } from 'react-icons/lu';
import { TbTournament } from 'react-icons/tb';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';

interface Props {
  tournamentId: string;
  pathname: string;
}

export default function AdminTournamentTabs({ tournamentId, pathname }: Props) {
  const base = `/admin/tournament/${tournamentId}`;

  const items: BottomNavItem[] = [
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
      href: `${base}/rounds/results`,
      label: 'Tableau',
      Icon: TbTournament,
      isActive: (p) => p.includes('/rounds/results'),
    },
  ];

  return <BottomNav items={items} pathname={pathname} />;
}