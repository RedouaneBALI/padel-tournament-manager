// app/pricing/page.tsx
'use client';

import { FiMoreHorizontal } from 'react-icons/fi';
import { Home } from 'lucide-react';
import { GiCrane } from 'react-icons/gi';
import { usePathname } from 'next/navigation';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import BackButton from '@/src/components/ui/buttons/BackButton';

export default function PricingPage() {
  const pathname = usePathname() ?? '';
  const items: BottomNavItem[] = [
    { href: '/', label: 'Accueil', Icon: Home, isActive: (p) => p === '/' },
    { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
  ];

  return (
    <>
      <main className="px-4 sm:px-6 py-4 pb-24">
        {/* Header: 3-column grid keeps title visually centered with a back button on the left */}
        <header className="grid grid-cols-3 items-center mb-6">
          <div className="justify-self-start">
            <BackButton />
          </div>
          <h1 className="justify-self-center text-2xl sm:text-3xl font-bold">Pricing</h1>
          <div className="justify-self-end" aria-hidden />
        </header>

        {/* Content */}
        <section className="max-w-md mx-auto">
          <div className="flex justify-center mb-4">
            <GiCrane className="text-6xl text-primary" />
          </div>
          <div className="text-center text-muted-foreground p-6">
            L'application est en cours de finalisation. Contactez-moi pour plus d'informations.
          </div>
        </section>
      </main>

      {/* Bottom navigation stays fixed at the bottom */}
      <BottomNav items={items} pathname={pathname} />
    </>
  );
}
