// app/admin/tournament/[id]/layout.tsx
'use client';

import React, { use, useCallback, useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useSession } from 'next-auth/react';
import { ToastContainer, toast } from 'react-toastify';
import { useExport } from '@/src/contexts/ExportContext';
import 'react-toastify/dist/ReactToastify.css';
import BottomNav from '@/src/components/ui/BottomNav';
import { getAdminTournamentItems } from '@/src/components/ui/bottomNavPresets';
import { fetchTournamentAdmin } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import PageHeader from '@/src/components/ui/PageHeader';

export default function AdminTournamentLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  return (
    <AdminTournamentLayoutContent
      children={children}
      params={params}
    />
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
  const { data: session } = useSession();
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);
  const router = useRouter();
  const pathname = usePathname() ?? '';
  const items = getAdminTournamentItems(id);
  const adminHeader = useExport();

  // Afficher le bouton retour uniquement sur les pages de détail de match (2 niveaux)
  // Ex: /tournament/4/games/81 ou /admin/tournament/4/games/81
  const isGameDetail = /\/tournament\/[^/]+\/games\/[^/]+$/.test(pathname);
  const isEditPage = /\/admin\/tournament\/[^/]+\/edit$/.test(pathname);
  const showBackButton = isGameDetail || isEditPage;

  let tvButtonUrl = tournament?.tvUrl ?? '';
  if (isGameDetail) {
    const match = pathname.match(/\/admin\/tournament\/[^/]+\/games\/([^/]+)/);
    if (match) {
      tvButtonUrl = `/tv/tournament/${id}/games/${match[1]}`;
    }
  }

  const showTvButton = isGameDetail || (tournament?.tvUrl !== null);

  const [exportFn, setExportFn] = useState<(() => void) | null>(null);

  const onExportRegister = useCallback((fn: (() => void) | null) => {
    setExportFn(() => fn);
  }, []);

  const handleCopyLink = useCallback(async () => {
    try {
      const publicPath = pathname.replace(`/admin/tournament/${id}`, `/tournament/${id}`);
      await navigator.clipboard.writeText(`${window.location.origin}${publicPath}${window.location.search}`);
      toast.success('Lien copié dans le presse-papiers !');
    } catch (err) {
      console.error('Failed to copy: ', err);
      toast.error('Erreur lors de la copie du lien.');
    }
  }, [id, pathname]);

  const handleEdit = useCallback(() => {
    router.push(`/admin/tournament/${id}/edit`);
  }, [router, id]);

  useEffect(() => {
    if (tournament && session?.user?.email) {
      const userEmail = session.user.email;
      const isOwner = tournament.ownerId === userEmail;
      const isEditor = (tournament.editorIds || []).includes(userEmail);
      const admin = isOwner || isEditor;

      setIsAdmin(admin);
    }
  }, [tournament, session?.user?.email]);

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

  useEffect(() => {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    adminHeader.setAdminActions({ isAdmin });
  }, [isAdmin]);

  useEffect(() => {
    const tvUrl = isGameDetail && id ? (() => {
      const match = pathname.match(/\/admin\/tournament\/[^/]+\/games\/([^/]+)/);
      return match ? `/tv/tournament/${id}/games/${match[1]}` : '';
    })() : (tournament?.tvUrl ?? '');

    // Don't override onExport - let TournamentGamesTab/TournamentResultsTab handle it
    // eslint-disable-next-line react-hooks/exhaustive-deps
    adminHeader.setAdminActions({
      showTvButton: isGameDetail || (tournament?.tvUrl !== null),
      tvButtonUrl: tvUrl || null,
      onShare: handleCopyLink,
      onEdit: handleEdit,
    });
  }, [tournament?.tvUrl, isGameDetail, id, pathname, handleCopyLink, handleEdit]);

  if (isLoading || !tournament) {
    return (
      <div className="w-full max-w-screen-2xl px-1 sm:px-4 mx-auto">
        <CenteredLoader />
        <ToastContainer />
      </div>
    );
  }


  return (
    <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
      <header className="pt-4 pb-2">
        <PageHeader
          title={tournament.name}
          showBackButton={showBackButton}
        />
      </header>
      <div className="mb-15">{children}</div>
      <BottomNav items={items} pathname={pathname} />
      <ToastContainer />
    </div>
  );
}
