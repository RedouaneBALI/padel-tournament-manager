'use client';

import { useRouter, usePathname } from 'next/navigation';
import { Settings, Share2 } from 'lucide-react';
import { toast } from 'react-toastify';
import Link from 'next/link';
import AdminTournamentTabs from './AdminTournamentTabs';
import type { Tournament } from '@/src/types/tournament';

interface Props {
  tournament: Tournament | null;
  tournamentId: string;
}

export default function AdminTournamentHeader({ tournament, tournamentId }: Props) {
  const router = useRouter();
  const pathname = usePathname();

  const handleCopyLink = () => {
    const shareUrl = `http://localhost:3000/tournament/${tournamentId}`;
    navigator.clipboard
      .writeText(shareUrl)
      .then(() => toast.success('Lien copié dans le presse-papiers !'))
      .catch(() => prompt('Copiez ce lien :', shareUrl));
  };

  return (
    <>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">{tournament?.name ?? 'Chargement...'}</h1>
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