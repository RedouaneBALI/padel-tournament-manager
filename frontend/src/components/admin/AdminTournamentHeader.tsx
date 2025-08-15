'use client';

import { useRouter, usePathname } from 'next/navigation';
import { Settings, Share2 } from 'lucide-react';
import { toast } from 'react-toastify';
import AdminTournamentTabs from './AdminTournamentTabs';
import type { Tournament } from '@/src/types/tournament';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface Props {
  tournament: Tournament | null;
  tournamentId: string;
}

export default function AdminTournamentHeader({ tournament, tournamentId }: Props) {
  const router = useRouter();
  const pathname = usePathname() ?? '';

  const handleCopyLink = () => {
    const FRONT_URL = (process.env.NEXT_PUBLIC_FRONTEND_URL ?? '').replace(/\/$/, '');
    const shareUrl = `${FRONT_URL}/tournament/${tournamentId}`;
    navigator.clipboard
      .writeText(shareUrl)
      .then(() => toast.success('Lien copié dans le presse-papiers !'))
      .catch(() => prompt('Copiez ce lien :', shareUrl));
  };

  return (
    <>
      <div className="flex items-center justify-between mb-4">
        <h1 className="flex flex-col">
          {tournament ? (
            <div className="flex items-center gap-3">
              <div className="w-1 h-10 bg-primary rounded"></div>
              <span className="text-2xl font-bold tracking-tight text-primary">
                {tournament.name}
              </span>
            </div>
          ) : (
            <CenteredLoader />
          )}
        </h1>
        <div className="flex items-center gap-2">
          <button
            onClick={handleCopyLink}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Partager le lien aux joueurs"
          >
            <Share2 className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
          <button
            onClick={() => router.push(`/admin/tournament/${tournamentId}/edit`)}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Modifier le tournoi"
          >
            <Settings className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
        </div>
      </div>

      <AdminTournamentTabs tournamentId={tournamentId} pathname={pathname} />
    </>
  );
}