'use client';

import React from 'react';
import { Settings, Share2, Download, Tv } from 'lucide-react';

interface AdminHeaderButtonsProps {
  showTvButton?: boolean;
  showDownloadButton?: boolean;
  onExport?: (() => void) | null;
  onShare?: (() => void) | null;
  onEdit?: (() => void) | null;
  tvButtonUrl?: string;
}

export default function AdminHeaderButtons({
  showTvButton = false,
  showDownloadButton = false,
  onExport,
  onShare,
  onEdit,
  tvButtonUrl,
}: AdminHeaderButtonsProps) {
  return (
    <div className="flex items-center gap-0 justify-center flex-shrink-0">
      {showTvButton && tvButtonUrl && (
        <button
          onClick={() => window.location.href = tvButtonUrl}
          className="p-1 rounded hover:bg-muted transition-colors cursor-pointer"
          title="Mode TV"
          aria-label="Mode TV"
        >
          <Tv className="h-5 w-5 text-muted-foreground hover:text-primary" />
        </button>
      )}
      {onShare && (
        <button
          onClick={onShare}
          className="p-1 rounded hover:bg-muted transition-colors cursor-pointer"
          title="Partager le lien aux joueurs"
        >
          <Share2 className="h-5 w-5 text-muted-foreground hover:text-primary" />
        </button>
      )}
      {showDownloadButton && onExport && (
        <button
          onClick={onExport}
          className="p-1 rounded hover:bg-muted transition-colors cursor-pointer"
          title="Télécharger"
        >
          <Download className="h-5 w-5 text-muted-foreground hover:text-primary" />
        </button>
      )}
      {onEdit && (
        <button
          onClick={onEdit}
          className="p-1 rounded hover:bg-muted transition-colors cursor-pointer"
          title="Modifier le tournoi"
        >
          <Settings className="h-5 w-5 text-muted-foreground hover:text-primary" />
        </button>
      )}
    </div>
  );
}
