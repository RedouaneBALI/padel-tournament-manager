'use client';

import React from 'react';
import BackButton from '@/src/components/ui/buttons/BackButton';
import { Settings, Share2, Download, Tv } from 'lucide-react';

interface PageHeaderAdminProps {
  title: React.ReactNode;
  showBackButton?: boolean;
  tournamentId?: string;
  showTvButton?: boolean;
  onExport?: (() => void) | null;
  onShare?: (() => void) | null;
  onEdit?: (() => void) | null;
  tvButtonUrl?: string;
  right?: React.ReactNode;
  loading?: boolean;
  className?: string;
}

export default function PageHeaderAdmin({
  title,
  showBackButton = false,
  tournamentId,
  showTvButton = false,
  onExport,
  onShare,
  onEdit,
  tvButtonUrl,
  right,
  loading = false,
  className = '',
}: PageHeaderAdminProps) {
  return (
    <div className="relative flex items-center mb-2 w-full">
      <BackButton className={showBackButton ? '' : 'invisible'} disabled={!showBackButton} />
      <h1 className="flex-1 justify-start overflow-hidden flex items-center gap-3 relative min-w-0">
        {loading ? (
          <span className="inline-block h-6 w-40 rounded bg-muted animate-pulse" />
        ) : (
          <span
            className="text-base font-semibold tracking-tight text-primary relative truncate overflow-hidden whitespace-nowrap block max-w-[calc(100%-120px)] after:absolute after:bottom-0 after:left-0 after:w-full after:h-0.5 after:bg-gradient-to-r after:from-[#1b2d5e] after:to-white"
          >
            {title}
          </span>
        )}
      </h1>
      <div className="absolute right-0 flex items-center gap-2 justify-center min-w-[140px] flex-shrink-0">
        {showTvButton && tvButtonUrl && (
          <button
            onClick={() => window.location.href = tvButtonUrl}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Mode TV"
            aria-label="Mode TV"
          >
            <Tv className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
        )}
        {onShare && (
          <button
            onClick={onShare}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Partager le lien aux joueurs"
          >
            <Share2 className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
        )}
        {onExport && (
          <button
            onClick={onExport}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Télécharger"
          >
            <Download className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
        )}
        {onEdit && (
          <button
            onClick={onEdit}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Modifier le tournoi"
          >
            <Settings className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
        )}
      </div>
      {right}
    </div>
  );
}
