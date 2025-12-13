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

  // Show download button only on Games (/games or /games/*) and Bracket (/bracket*) pages
  const showDownloadButton = /\/games($|\/)/.test(pathname) || /\/bracket/.test(pathname);

  return (
    <AdminHeaderButtons
      showTvButton={showTvButton}
      showDownloadButton={showDownloadButton}
      onExport={onExport}
      onShare={onShare}
      onEdit={onEdit}
      tvButtonUrl={tvButtonUrl}
    />
  );
}
