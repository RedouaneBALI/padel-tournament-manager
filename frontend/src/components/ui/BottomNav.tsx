'use client';

import Link from 'next/link';
import { useMemo, useState, useCallback } from 'react';
import { useSession } from 'next-auth/react';
import { FiLogIn } from 'react-icons/fi';
import LogoutButton from '@/src/components/auth/LogoutButton';
import CreateTournamentButton from '@/src/components/ui/buttons/CreateTournamentButton';
import MyTournamentsButton from '@/src/components/ui/buttons/MyTournamentsButton';
import ContactButton from '@/src/components/ui/buttons/ContactButton';
import GoogleLoginButton from '@/src/components/ui/buttons/GoogleLoginButton';

export type BottomNavItem = {
  href: string;
  label: string;
  Icon: React.ComponentType<{ className?: string }>;
  isActive?: (pathname: string) => boolean;
};

interface BottomNavProps {
  items: BottomNavItem[];
  pathname: string;
  className?: string;
  fixed?: boolean; // default true
}

export default function BottomNav({ items, pathname, className, fixed = true }: BottomNavProps) {
  const colsClass = useMemo(() => {
    const n = Math.max(1, Math.min(items.length, 6));
    return `grid-cols-${n}` as const;
  }, [items.length]);

  const [moreOpen, setMoreOpen] = useState(false);
  const closeMore = useCallback(() => setMoreOpen(false), []);

  const { status } = useSession();

  const hrefCreate = status === 'authenticated' ? '/admin/tournament/new' : '/connexion';
  const hrefMy = status === 'authenticated' ? '/admin/tournaments' : '/connexion';

  const computeActive = (href: string, isActive?: (p: string) => boolean) => {
    if (href === '#more') return false;
    if (isActive) return isActive(pathname);
    if (pathname === href) return true;
    // Only treat prefix as active if no more specific item also matches
    if (pathname.startsWith(href + '/')) {
      const moreSpecific = items.some(it => it.href !== href && (pathname === it.href || pathname.startsWith(it.href + '/')));
      return !moreSpecific;
    }
    return false;
  };

  return (
    <>
      <nav
        className={[
          fixed ? 'fixed bottom-0 inset-x-0' : '',
          'border-t border-border bg-background z-50',
          'grid grid-flow-col',
          colsClass,
          className ?? '',
        ].join(' ').trim()}
        role="navigation"
        aria-label="Navigation principale"
      >
        {items.map(({ href, label, Icon, isActive }, idx) => {
          if (href === '#more') {
            return (
              <button
                key={idx}
                type="button"
                className={`relative flex flex-col items-center justify-center py-2 text-xs text-muted-foreground hover:text-primary`}
                onClick={() => setMoreOpen(true)}
              >
                <Icon className="h-5 w-5" />
                <span className="leading-none mt-2">{label}</span>
              </button>
            );
          }
          const active = computeActive(href, isActive);
          return (
            <Link
              key={idx}
              href={href}
              className={`relative flex flex-col items-center justify-center py-2 text-xs ${
                active ? 'text-primary' : 'text-muted-foreground hover:text-primary'
              }`}
              aria-current={active ? 'page' : undefined}
            >
              <Icon className="h-5 w-5" />
              <span className="leading-none mt-2">{label}</span>
              {active && <span className="absolute bottom-1 w-3/4 h-0.5 bg-primary rounded-full" />}
            </Link>
          );
        })}
      </nav>
      {moreOpen && (
        <>
          <div className="fixed inset-0 bg-black/50 z-[60]" onClick={closeMore} />
          <div className="fixed inset-x-0 bottom-0 z-[70] bg-background rounded-t-2xl border-t border-border shadow-2xl">
            <div className="max-w-screen-sm mx-auto p-4">
              <div className="h-1.5 w-10 bg-muted-foreground/40 rounded-full mx-auto mb-4" />
              <div className="flex flex-col gap-2">
                <CreateTournamentButton href={hrefCreate} onClick={closeMore} />
                <MyTournamentsButton href={hrefMy} onClick={closeMore} />
                <ContactButton onClick={closeMore} />
                <div className="mt-2" role="none">
                  {status === 'authenticated' ? (
                    <div onClick={closeMore}>
                      <LogoutButton className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground" iconClassName="w-6 h-6 flex-none">
                        Déconnexion
                      </LogoutButton>
                    </div>
                  ) : (
                    <GoogleLoginButton onBeforeSignIn={closeMore} />
                  )}
                </div>
              </div>
              <div className="pb-6" />
            </div>
          </div>
        </>
      )}
    </>
  );
}
