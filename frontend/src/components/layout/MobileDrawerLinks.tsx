'use client';

import Link from 'next/link';
import LogoutButton from '@/src/components/auth/LogoutButton';
import { FiPlusCircle, FiList, FiMail } from 'react-icons/fi';
import { useCallback } from 'react';

export default function MobileDrawerLinks() {
  const handleClick = useCallback((e: React.MouseEvent<HTMLElement>) => {
    const el = e.currentTarget as HTMLElement;
    const details = el.closest('details') as HTMLDetailsElement | null;
    if (details) details.open = false;
  }, []);

  return (
    <nav className="flex flex-col gap-2">
      <Link
        href="/admin/tournament/new"
        className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground"
        aria-label="Créer un tournoi"
        title="Créer un tournoi"
        onClick={handleClick}
      >
        <FiPlusCircle className="w-6 h-6 flex-none" />
        Créer un tournoi
      </Link>
      <Link
        href="/admin/tournaments"
        className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground"
        aria-label="Mes tournois"
        title="Mes tournois"
        onClick={handleClick}
      >
        <FiList className="w-6 h-6 flex-none" />
        Mes tournois
      </Link>
      <Link
        href="/contact"
        className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground"
        aria-label="Contact"
        title="Contact"
        onClick={handleClick}
      >
        <FiMail className="w-6 h-6 flex-none" />
        Contact
      </Link>
      <div className="mt-2" onClick={handleClick} role="none">
        <LogoutButton
          className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground"
          iconClassName="w-6 h-6 flex-none"
        >
          Déconnexion
        </LogoutButton>
      </div>
    </nav>
  );
}
