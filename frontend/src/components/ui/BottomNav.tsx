'use client';

import Link from 'next/link';
import { useMemo, useState, useCallback } from 'react';
import { useSession } from 'next-auth/react';
import { FiLogIn } from 'react-icons/fi';
import LogoutButton from '@/src/components/auth/LogoutButton';
import CreateTournamentButton from '@/src/components/ui/buttons/CreateTournamentButton';
import CreateGameButton from '@/src/components/ui/buttons/CreateGameButton';
import MyTournamentsButton from '@/src/components/ui/buttons/MyTournamentsButton';
import MyGamesButton from '@/src/components/ui/buttons/MyGamesButton';
import ContactButton from '@/src/components/ui/buttons/ContactButton';
import GoogleLoginButton from '@/src/components/ui/buttons/GoogleLoginButton';
import PricingButton from '@/src/components/ui/buttons/PricingButton';
import PointsCalculatorButton from '@/src/components/ui/buttons/PointsCalculatorButton';
import RankingButton from '@/src/components/ui/buttons/RankingButton';
import MyAccountButton from '@/src/components/ui/buttons/MyAccountButton';
import LatestTournamentsButton from '@/src/components/ui/buttons/LatestTournamentsButton';

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
  onMoreClick?: () => void;
  isMoreOpen?: boolean;
}

export default function BottomNav({ items, pathname, className, fixed = true, onMoreClick, isMoreOpen }: BottomNavProps) {
  const colsClass = useMemo(() => {
    const n = Math.max(1, Math.min(items.length, 6));
    return `grid-cols-${n}` as const;
  }, [items.length]);

  const [moreOpen, setMoreOpen] = useState(false);
  const closeMore = useCallback(() => setMoreOpen(false), []);

  const { status } = useSession();

  const hrefCreateTournament = status === 'authenticated' ? '/admin/tournament/new' : '/connexion';
  const hrefCreateGame = status === 'authenticated' ? '/admin/game/new' : '/connexion';
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
            const active = isMoreOpen ?? moreOpen;
            return (
              <button
                key={idx}
                type="button"
                className={`relative flex flex-col items-center justify-center py-2 text-xs ${
                  active ? 'text-primary' : 'text-muted-foreground'
                } hover:text-primary`}
                onClick={onMoreClick ?? (() => setMoreOpen(true))}
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
      {(isMoreOpen ?? moreOpen) && (
        <>
          <div className="fixed inset-0 bg-black/50 z-[60]" onClick={onMoreClick ?? closeMore} />
          <div className="fixed inset-x-0 bottom-0 z-[70] bg-background rounded-t-2xl border-t border-border shadow-2xl max-h-[90vh]">
            <div className="max-w-screen-sm mx-auto p-4 pt-3 relative">
              {/* Croix flottante dans le coin supérieur droit */}
              <button
                onClick={onMoreClick ?? closeMore}
                className="absolute top-2 right-2 p-2 hover:bg-accent/50 active:bg-accent rounded-full transition-colors touch-manipulation z-10"
                aria-label="Fermer le menu"
              >
                <svg className="w-6 h-6 text-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>

              <div className="flex flex-col">
                <h3 className="text-sm font-semibold text-muted-foreground mt-4 mb-1 first:mt-0">Administration</h3>
                <hr className="border-t border-border my-1" />
                <div className="flex flex-col gap-2">
                  <CreateTournamentButton href={hrefCreateTournament} onClick={onMoreClick ?? closeMore} />
                  <CreateGameButton href={hrefCreateGame} onClick={onMoreClick ?? closeMore} />
                  <MyTournamentsButton href={hrefMy} onClick={onMoreClick ?? closeMore} />
                  <MyGamesButton href={hrefMy.replace('tournaments','games')} onClick={onMoreClick ?? closeMore} />
                </div>

                <h3 className="text-sm font-semibold text-muted-foreground mt-4 mb-1">FRMT 🇲🇦</h3>
                <hr className="border-t border-border my-1" />
                <div className="flex flex-col gap-2">
                  <PointsCalculatorButton onClick={onMoreClick ?? closeMore} />
                  <RankingButton onClick={onMoreClick ?? closeMore} />
                </div>

                <h3 className="text-sm font-semibold text-muted-foreground mt-4 mb-1">Général</h3>
                <hr className="border-t border-border my-1" />
                <div className="flex flex-col gap-2">
                  <LatestTournamentsButton onClick={onMoreClick ?? closeMore} />
                  <PricingButton onClick={onMoreClick ?? closeMore} />
                  <ContactButton onClick={onMoreClick ?? closeMore} />
                  {status === 'authenticated' && <MyAccountButton onClick={onMoreClick ?? closeMore} />}
                  <div role="none">
                    {status === 'authenticated' ? (
                      <div onClick={onMoreClick ?? closeMore}>
                        <LogoutButton>
                          <span className="text-sm">Déconnexion</span>
                        </LogoutButton>
                      </div>
                    ) : (
                      <GoogleLoginButton onBeforeSignIn={onMoreClick ?? closeMore} />
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
