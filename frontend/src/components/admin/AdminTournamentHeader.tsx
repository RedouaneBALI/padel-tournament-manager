//AdminTournamentHeader.tsx
'use client';

import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { Settings, Share2, Download, Tv } from 'lucide-react';
import { toast } from 'react-toastify';
import AdminTournamentTabs from './AdminTournamentTabs';
import type { Tournament } from '@/src/types/tournament';
import PageHeader from '@/src/components/ui/PageHeader';
import { useExport } from '@/src/contexts/ExportContext';

interface Props {
  tournament: Tournament | null;
  tournamentId?: string;
}

export default function AdminTournamentHeader({ tournament, tournamentId }: Props) {
  const router = useRouter();
  const pathname = usePathname() ?? '';
  const searchParams = useSearchParams();
  const { onExport } = useExport();

  // Déterminer localement si on doit afficher le bouton TV
  const isStandaloneAdminGame = pathname.startsWith('/admin/game/');
  const isTournamentGameRoute = !!tournamentId && pathname.includes('/games/');
  const showTvButton = isStandaloneAdminGame || isTournamentGameRoute;

  // Calculer l'URL du mode TV en fonction du chemin
  let tvButtonUrl: string | undefined;
  if (isStandaloneAdminGame) {
    const match = pathname.match(/^\/admin\/game\/([^\/?#]+)/);
    const gameIdFromPath = match ? match[1] : undefined;
    if (gameIdFromPath) tvButtonUrl = `/tv/game/${gameIdFromPath}`;
  } else if (isTournamentGameRoute && tournamentId) {
    const suffix = pathname.split(`/admin/tournament/${tournamentId}`)[1] || '';
    tvButtonUrl = `/tv/tournament/${tournamentId}${suffix}`;
  }

  const handleCopyLink = () => {
    const FRONT_URL = (process.env.NEXT_PUBLIC_FRONTEND_URL ?? '').replace(/\/$/, '');
    // Remplacer /admin par / en gardant le reste du pathname
    let publicPathname = pathname.replace(/^\/admin\//, '/');

    if (publicPathname.includes('/rounds/config')) {
      publicPathname = `/tournament/${tournamentId}`;
    }

    // Ajouter les query parameters s'ils existent
    const queryString = searchParams?.toString() ?? '';
    const shareUrl = queryString
      ? `${FRONT_URL}${publicPathname}?${queryString}`
      : `${FRONT_URL}${publicPathname}`;

    navigator.clipboard
      .writeText(shareUrl)
      .then(() => toast.success('Lien copié dans le presse-papiers !'))
      .catch(() => prompt('Copiez ce lien :', shareUrl));
  };

  return (
    <>
      <div className="flex items-center justify-between">
        <h1 className="flex flex-col">
          {tournamentId && tournament ? (
            <header className="pt-4 pb-2">
              <PageHeader title={tournament.name} />
            </header>
          ) : null}
        </h1>
        <div className="flex items-center gap-2">
          {showTvButton && tvButtonUrl && (
            <button
              onClick={() => router.push(tvButtonUrl)}
              className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
              title="Mode TV"
              aria-label="Mode TV"
            >
              <Tv className="h-5 w-5 text-muted-foreground hover:text-primary" />
            </button>
          )}
          <button
            onClick={handleCopyLink}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Partager le lien aux joueurs"
          >
            <Share2 className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
          {tournamentId && onExport && (
            <button
              onClick={onExport}
              className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
              title="Télécharger"
            >
              <Download className="h-5 w-5 text-muted-foreground hover:text-primary" />
            </button>
          )}
          {tournamentId && (
            <button
              onClick={() => router.push(`/admin/tournament/${tournamentId}/edit`)}
              className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
              title="Modifier le tournoi"
            >
              <Settings className="h-5 w-5 text-muted-foreground hover:text-primary" />
            </button>
          )}
        </div>
      </div>

      {tournamentId && (
        <AdminTournamentTabs tournamentId={tournamentId} pathname={pathname} />
      )}
    </>
  );
}