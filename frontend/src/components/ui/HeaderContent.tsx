'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import { useExport } from '@/src/contexts/ExportContext';
import HeaderAdminActions from '@/src/components/ui/HeaderAdminActions';

export default function HeaderContent() {
  const { onExport, onShare, onEdit, tvButtonUrl, showTvButton, setAdminActions, tournamentName, setTournamentName } = useExport();
  const pathname = usePathname();
  const hasAdminActions = !!(onExport || onShare || onEdit || showTvButton);

  React.useEffect(() => {
    const isTournament = pathname && (pathname.startsWith('/tournament/') || pathname.startsWith('/admin/tournament/'));
    if (!pathname || !isTournament) {
      setAdminActions({ onExport: null, onShare: null, onEdit: null, tvButtonUrl: null, showTvButton: false, isAdmin: false });
      setTournamentName(null);
    }
  }, [pathname, setAdminActions, setTournamentName]);
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
      {tournamentName && (
        hasAdminActions ? (
          <span className="text-base font-semibold tracking-tight text-primary truncate overflow-hidden whitespace-nowrap block after:absolute after:bottom-0 after:left-0 after:w-full after:h-0.5 after:bg-gradient-to-r after:from-[#1b2d5e] after:to-white relative text-center">
            {tournamentName}
          </span>
        ) : (
          <div className="absolute left-1/2 transform -translate-x-1/2">
            <span className="text-base font-semibold tracking-tight text-primary truncate overflow-hidden whitespace-nowrap block after:absolute after:bottom-0 after:left-0 after:w-full after:h-0.5 after:bg-gradient-to-r after:from-[#1b2d5e] after:to-white relative">
              {tournamentName}
            </span>
          </div>
        )
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
