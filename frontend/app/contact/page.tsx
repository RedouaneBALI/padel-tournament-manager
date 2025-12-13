// app/contact/page.tsx
'use client';

import { FiMail, FiLinkedin, FiInstagram, FiMoreHorizontal } from 'react-icons/fi';
import { Home } from 'lucide-react';
import { usePathname } from 'next/navigation';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import BackButton from '@/src/components/ui/buttons/BackButton';

export default function ContactPage() {
  const pathname = usePathname() ?? '';
  const items: BottomNavItem[] = [
    { href: '/', label: 'Accueil', Icon: Home, isActive: (p) => p === '/' },
    { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
  ];

  return (
    <>
      <main>
        {/* Header: 3-column grid keeps title visually centered with a back button on the left */}
        <header className="grid grid-cols-3 items-center mb-6">
          <div className="justify-self-start">
            <BackButton />
          </div>
          <h1 className="justify-self-center text-2xl sm:text-3xl font-bold">Contact</h1>
          <div className="justify-self-end" aria-hidden />
        </header>

        {/* Content */}
        <section className="max-w-md mx-auto">
          <div className="grid grid-cols-1 gap-6">
            <a
              href="mailto:bali.redouane@gmail.com"
              className="group flex items-center gap-4 rounded-xl border border-border bg-card hover:bg-accent hover:text-accent-foreground transition-colors p-4"
            >
              <span className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-white/70 group-hover:bg-background">
                <FiMail size={28} className="text-brand-google" />
              </span>
              <div>
                <div className="font-medium">Email</div>
                <div className="text-sm text-muted-foreground">bali.redouane@gmail.com</div>
              </div>
            </a>

            <a
              href="https://www.linkedin.com/in/redouane-bali/"
              target="_blank"
              rel="noopener noreferrer"
              className="group flex items-center gap-4 rounded-xl border border-border bg-card hover:bg-accent hover:text-accent-foreground transition-colors p-4"
            >
              <span className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-white/70 group-hover:bg-background">
                <FiLinkedin size={28} className="text-brand-linkedin" />
              </span>
              <div>
                <div className="font-medium">LinkedIn</div>
                <div className="text-sm text-muted-foreground">/in/redouane-bali</div>
              </div>
            </a>

            <a
              href="https://www.instagram.com/padelrounds/"
              target="_blank"
              rel="noopener noreferrer"
              className="group flex items-center gap-4 rounded-xl border border-border bg-card hover:bg-accent hover:text-accent-foreground transition-colors p-4"
            >
              <span className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-white/70 group-hover:bg-background">
                <FiInstagram size={28} className="text-brand-instagram" />
              </span>
              <div>
                <div className="font-medium">Instagram</div>
                <div className="text-sm text-muted-foreground">@padelrounds</div>
              </div>
            </a>
          </div>
        </section>
      </main>

      {/* Bottom navigation stays fixed at the bottom */}
      <BottomNav items={items} pathname={pathname} />
    </>
  );
}
