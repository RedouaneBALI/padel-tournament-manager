'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useExport } from '@/src/contexts/ExportContext';
import HeaderAdminActions from '@/src/components/ui/HeaderAdminActions';

export default function HeaderContent() {
  const { onExport, onShare, onEdit, tvButtonUrl, showTvButton } = useExport();
  const hasAdminActions = !!(onExport || onShare || onEdit || showTvButton);


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

