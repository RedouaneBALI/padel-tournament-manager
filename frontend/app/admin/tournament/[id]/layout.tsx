// app/admin/tournament/[id]/layout.tsx
'use client';

import React, { use, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { ToastContainer, toast } from 'react-toastify';
import { ExportProvider, useExport } from '@/src/contexts/ExportContext';
import 'react-toastify/dist/ReactToastify.css';
import BottomNav from '@/src/components/ui/BottomNav';
import { getAdminTournamentItems } from '@/src/components/ui/bottomNavPresets';
import { fetchTournamentAdmin } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import PageHeaderAdmin from '@/src/components/ui/PageHeaderAdmin';

export default function AdminTournamentLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  // On place ExportProvider ici pour englober tout le layout et permettre l'usage du contexte
  return (
    <ExportProvider>
      <AdminTournamentLayoutContent
        children={children}
        params={params}
      />
    </ExportProvider>
  );
}

function AdminTournamentLayoutContent({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname() ?? '';
  const items = getAdminTournamentItems(id);
  const { onExport } = useExport();

  // Afficher le bouton retour uniquement sur les pages de détail de match (2 niveaux)
  // Ex: /tournament/4/games/81 ou /admin/tournament/4/games/81
  const isGameDetail = /\/tournament\/[^/]+\/games\/[^/]+$/.test(pathname);
  const isEditPage = /\/admin\/tournament\/[^/]+\/edit$/.test(pathname);
  const showBackButton = isGameDetail || isEditPage;

  useEffect(() => {
    let mounted = true;

    async function loadTournament() {
      try {
        const data = await fetchTournamentAdmin(id);
        if (!mounted) return;

        // Backend now returns isEditable correctly
        if (data.isEditable !== true) {
          const subPath = pathname.replace(`/admin/tournament/${id}`, '');
          const search = window.location.search;
          router.replace(`/tournament/${id}${subPath}${search}`);
          return;
        }

        setTournament(data);
      } catch (e: any) {
        if (!mounted) return;

        if (e?.message === 'FORBIDDEN' || e?.message === 'UNAUTHORIZED') {
          const subPath = pathname.replace(`/admin/tournament/${id}`, '');
          const search = window.location.search;
          router.replace(`/tournament/${id}${subPath}${search}`);
          return;
        }

        console.error("Failed to load tournament:", e);
      } finally {
        if (mounted) {
          setIsLoading(false);
        }
      }
    }

    loadTournament();
    return () => { mounted = false; };
  }, [id, router, pathname]);

  if (isLoading || !tournament) {
    return (
      <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
        <CenteredLoader />
        <ToastContainer />
      </div>
    );
  }

  const showTvButton = tournament.tvUrl !== null;
  const tvButtonUrl = tournament.tvUrl ?? '';
  const handleCopyLink = async () => {
    try {
      const publicPath = pathname.replace(`/admin/tournament/${id}`, `/tournament/${id}`);
      await navigator.clipboard.writeText(`${window.location.origin}${publicPath}${window.location.search}`);
      toast.success('Lien copié dans le presse-papiers !');
    } catch (err) {
      console.error('Failed to copy: ', err);
      toast.error('Erreur lors de la copie du lien.');
    }
  };

  return (
    <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
      <PageHeaderAdmin
        title={tournament.name}
        showBackButton={showBackButton}
        tournamentId={id}
        showTvButton={showTvButton}
        onExport={onExport}
        onShare={handleCopyLink}
        onEdit={() => router.push(`/admin/tournament/${id}/edit`)}
        tvButtonUrl={tvButtonUrl}
      />
      <div className="mb-15">{children}</div>
      <BottomNav items={items} pathname={pathname} />
      <ToastContainer />
    </div>
  );
}
