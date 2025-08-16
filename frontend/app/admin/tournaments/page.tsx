// app/admin/tournaments/page.tsx
'use client';

import TournamentList from '@/src/components/tournament/TournamentList';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { usePathname } from 'next/navigation';
import { Home } from 'lucide-react';
import { FiMoreHorizontal } from 'react-icons/fi';
import { ToastContainer } from 'react-toastify';

export default function TournamentsPage() {
  const pathname = usePathname() ?? '';
  const items: BottomNavItem[] = [
    { href: '/', label: 'Accueil', Icon: Home, isActive: (p) => p === '/' },
    { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
  ];

  return (
    <>
      <main className="min-h-screen bg-background">
        <section className="max-w-5xl mx-auto mb-4">
          <TournamentList />
        </section>
      </main>
      <BottomNav items={items} pathname={pathname} />
      <ToastContainer />
    </>
  );
}