'use client';

import React from 'react';
import BackButton from '@/src/components/ui/buttons/BackButton';
import { Settings, Share2, Download, Tv } from 'lucide-react';

interface TournamentHeaderProps {
  title: React.ReactNode;
  showBackButton?: boolean;
  admin?: boolean;
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

export default function TournamentHeader({
  title,
  showBackButton = false,
  admin = false,
  tournamentId,
  showTvButton = false,
  onExport,
  onShare,
  onEdit,
  tvButtonUrl,
  right,
  loading = false,
  className = '',
}: TournamentHeaderProps) {
  return (
    <div className={`flex items-center justify-between mb-2 ${className}`}>
      <div className="flex items-center gap-2">
        {showBackButton && <BackButton />}
        <h1 className="flex items-center gap-3 relative">
          {/* Soulignement animé sous le titre */}
          {loading ? (
            <span className="inline-block h-6 w-40 rounded bg-muted animate-pulse" />
          ) : (
            <span className="text-2xl font-bold tracking-tight text-primary relative">
              {title}
              <span className="block absolute left-0 right-0 -bottom-1 h-1 rounded bg-gradient-to-r from-primary to-blue-400 opacity-70 animate-pulse" style={{transition: 'width 0.3s'}} />
            </span>
          )}
        </h1>
      </div>
      {admin && (
        <div className="flex items-center gap-2 justify-center min-w-[140px]">
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
      )}
      {right}
    </div>
  );
}