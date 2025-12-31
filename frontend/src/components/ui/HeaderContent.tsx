'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname, useRouter } from 'next/navigation';
import { useExport } from '@/src/contexts/ExportContext';
import { useFavorites } from '@/src/hooks/useFavorites';
import { FaStar, FaRegStar, FaChevronRight, FaUserShield } from 'react-icons/fa';
import HeaderAdminActions from '@/src/components/ui/HeaderAdminActions';
import { useSession } from 'next-auth/react';

export default function HeaderContent() {
  const { onExport, onShare, onEdit, tvButtonUrl, showTvButton, setAdminActions, tournamentName, setTournamentName, canSwitchToAdmin } = useExport();
  const pathname = usePathname();
  const router = useRouter();
  const hasAdminActions = !!(onExport || onShare || onEdit || showTvButton);
  const { status } = useSession();

  // Extract tournament ID from pathname
  const tournamentId = React.useMemo(() => {
    const match = pathname?.match(/\/(?:admin\/)?tournament\/(\d+)/);
    return match ? Number.parseInt(match[1]) : null;
  }, [pathname]);

  const isTournamentPage = pathname ? pathname.startsWith('/tournament/') && !pathname.startsWith('/admin/tournament/') : false;
  const { favoriteTournaments, toggleFavoriteTournament } = useFavorites(isTournamentPage);

  const isFavorite = tournamentId && favoriteTournaments ? favoriteTournaments.some(t => t.id === tournamentId) : false;

  const handleToggleFavorite = () => {
    if (status !== 'authenticated') {
      if (typeof window !== 'undefined' && pathname) {
        localStorage.setItem('authReturnUrl', pathname);
      }
      router.push('/connexion');
    } else if (tournamentId && favoriteTournaments) {
      toggleFavoriteTournament(tournamentId, isFavorite);
    }
  };

  React.useEffect(() => {
    if (pathname === '/favorites') {
      setAdminActions({ onExport: null, onShare: null, onEdit: null, tvButtonUrl: null, showTvButton: false, isAdmin: false, canSwitchToAdmin: false });
      setTournamentName("Mes favoris");
    } else if (!pathname || (!pathname.startsWith('/tournament/') && !pathname.startsWith('/admin/tournament/'))) {
      setAdminActions({ onExport: null, onShare: null, onEdit: null, tvButtonUrl: null, showTvButton: false, isAdmin: false, canSwitchToAdmin: false });
      setTournamentName(null);
    }
  }, [pathname, setAdminActions, setTournamentName]);

  const [showChevron, setShowChevron] = React.useState(false);

  const prevPathnameRef = React.useRef<string | null>(null);

  React.useEffect(() => {
    if (prevPathnameRef.current === '/favorites') {
      setShowChevron(true);
    } else {
      setShowChevron(false);
    }
    prevPathnameRef.current = pathname;
  }, [pathname]);

  const tournamentGamesUrl = tournamentId ? `/tournament/${tournamentId}/games` : '';
  const adminSwitchUrl = tournamentId && pathname ? `/admin/tournament/${tournamentId}${pathname.replace(`/tournament/${tournamentId}`, '')}` : '';

  return (
    <div className={`relative flex items-center w-full ${hasAdminActions ? 'grid grid-cols-[auto_1fr_auto] gap-2' : ''}`}>
      <Link href="/" className="flex items-center gap-2" aria-label="Accueil" title="Accueil">
        <Image
          src="/pr-logo.png"
          alt="Padel Rounds"
          width={32}
          height={32}
          priority
          className="h-12 w-auto"
        />
        <span className="sr-only">Accueil</span>
      </Link>
      {tournamentName && tournamentId && (
        hasAdminActions ? (
          <button
            onClick={() => router.push(tournamentGamesUrl)}
            className="text-base font-semibold tracking-tight text-primary truncate overflow-hidden whitespace-nowrap block after:absolute after:bottom-0 after:left-0 after:w-full after:h-0.5 after:bg-gradient-to-r after:from-[#1b2d5e] after:to-white relative text-center hover:opacity-80 transition-opacity cursor-pointer"
          >
            {tournamentName}
          </button>
        ) : (
          <button
            onClick={() => router.push(tournamentGamesUrl)}
            className="absolute left-1/2 transform -translate-x-1/2 text-base font-semibold tracking-tight text-primary truncate overflow-hidden whitespace-nowrap flex items-center gap-1 hover:text-primary transition-colors hover:opacity-80 transition-opacity cursor-pointer"
          >
            {tournamentName}
            {showChevron && <FaChevronRight className="h-4 w-4" />}
          </button>
        )
      )}
      {!hasAdminActions && tournamentId && canSwitchToAdmin && pathname && (
        <button
          onClick={() => router.push(adminSwitchUrl)}
          className="absolute top-1/2 right-12 -translate-y-1/2 p-1 rounded hover:bg-muted transition-colors cursor-pointer"
          title="Passer en mode admin"
          aria-label="Passer en mode admin"
        >
          <FaUserShield className="h-5 w-5 text-muted-foreground hover:text-primary" />
        </button>
      )}
      {!hasAdminActions && tournamentId && status === 'authenticated' && (
        <button
          onClick={handleToggleFavorite}
          className="absolute top-1/2 right-0 -translate-y-1/2 p-1 rounded hover:bg-muted transition-colors cursor-pointer"
          title={isFavorite ? 'Retirer des favoris' : 'Ajouter aux favoris'}
          aria-label={isFavorite ? 'Retirer des favoris' : 'Ajouter aux favoris'}
        >
          {isFavorite ? (
            <FaStar className="h-5 w-5 fill-yellow-400 text-yellow-400" />
          ) : (
            <FaRegStar className="h-5 w-5 text-muted-foreground hover:text-primary" />
          )}
        </button>
      )}
      {hasAdminActions && (
        <HeaderAdminActions
          onExport={onExport}
          onShare={onShare}
          onEdit={onEdit}
          tvButtonUrl={tvButtonUrl ?? undefined}
          showTvButton={showTvButton}
        />
      )}
    </div>
  );
}
