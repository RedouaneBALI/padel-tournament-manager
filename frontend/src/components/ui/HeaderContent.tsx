'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import { useExport } from '@/src/contexts/ExportContext';
import HeaderAdminActions from '@/src/components/ui/HeaderAdminActions';

export default function HeaderContent() {
  const { onExport, onShare, onEdit, tvButtonUrl, showTvButton, setAdminActions } = useExport();
  const pathname = usePathname();
  const hasAdminActions = !!(onExport || onShare || onEdit || showTvButton);

  React.useEffect(() => {
    const isTournament = pathname.startsWith('/tournament/') || pathname.startsWith('/admin/tournament/');
    if (!isTournament) {
      setAdminActions({ onExport: null, onShare: null, onEdit: null, tvButtonUrl: null, showTvButton: false, isAdmin: false });
    }
  }, [pathname, setAdminActions]);

  return (
    <>
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
      {hasAdminActions && (
        <div className="absolute right-0">
          <HeaderAdminActions
            onExport={onExport}
            onShare={onShare}
            onEdit={onEdit}
            tvButtonUrl={tvButtonUrl ?? undefined}
            showTvButton={showTvButton}
          />
        </div>
      )}
    </>
  );
}
