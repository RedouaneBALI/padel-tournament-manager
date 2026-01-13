'use client';

import React from 'react';
import { usePathname } from 'next/navigation';
import { useExport } from '@/src/contexts/ExportContext';
import AdminHeaderButtons from './AdminHeaderButtons';

interface HeaderAdminActionsProps {
  onExport?: (() => void) | null;
  onShare?: (() => void) | null;
  onEdit?: (() => void) | null;
  tvButtonUrl?: string;
  showTvButton?: boolean;
}

export default function HeaderAdminActions({
  onExport,
  onShare,
  onEdit,
  tvButtonUrl,
  showTvButton = false,
}: HeaderAdminActionsProps) {
  const { isAdmin } = useExport();
  const pathname = usePathname() ?? '';

  if (!isAdmin) {
    return null;
  }

  // Hide admin actions on edit pages
  if (pathname.includes('/edit')) {
    return null;
  }

  // Show download button only on Games (/games) and Bracket (/bracket*) pages, but not on game detail pages (/games/XX)
  const showDownloadButton = /\/games$/.test(pathname) || /\/bracket/.test(pathname);

  // Hide edit button on game detail pages (/games/XX)
  const isGameDetailPage = /\/games\/\d+/.test(pathname);
  const showEditButton = !isGameDetailPage;

  return (
    <AdminHeaderButtons
      showTvButton={showTvButton}
      showDownloadButton={showDownloadButton}
      onExport={onExport}
      onShare={onShare}
      onEdit={showEditButton ? onEdit : null}
      tvButtonUrl={tvButtonUrl}
    />
  );
}
