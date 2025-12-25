'use client';

import React from 'react';
import PageHeader from '@/src/components/ui/PageHeader';
import { useFavorites } from '@/src/hooks/useFavorites';
import BottomNav from '@/src/components/ui/BottomNav';
import { usePathname, useSearchParams, useRouter } from 'next/navigation';
import { Home } from 'lucide-react';
import { FiMoreHorizontal } from 'react-icons/fi';
import FavoriteTournamentsList from '@/src/components/favorites/FavoriteTournamentsList';
import FavoriteGamesList from '@/src/components/favorites/FavoriteGamesList';
import { useSession } from 'next-auth/react';

type TabType = 'tournaments' | 'games';

export default function FavoritesPage() {
  const { status } = useSession();
  const router = useRouter();

  React.useEffect(() => {
    if (status === 'unauthenticated') {
      const currentPath = window.location.pathname + window.location.search;
      localStorage.setItem('authReturnUrl', currentPath);
      router.push('/connexion');
    }
  }, [status, router]);

  const { favoriteTournaments, favoriteGames, loading, error, toggleFavoriteGame } = useFavorites(true);
  const searchParams = useSearchParams();
  const pathname = usePathname();

  const activeTab = React.useMemo(() => {
    if (!searchParams) return 'tournaments';
    return (searchParams.get('tab') as TabType) || 'tournaments';
  }, [searchParams]);

  const updateQuery = React.useCallback((key: string, value: string) => {
    if (!searchParams || !pathname) return;
    const params = new URLSearchParams(searchParams.toString());
    params.set(key, value);
    router.push(`${pathname}?${params.toString()}`);
  }, [searchParams, pathname, router]);

  const items = React.useMemo(() => [
    { href: '/', label: 'Accueil', Icon: Home, isActive: (p: string) => p === '/' },
    { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
  ], []);

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <PageHeader loading />
        <div>
          <div className="animate-pulse space-y-4">
            <div className="h-4 bg-muted rounded w-3/4"></div>
            <div className="h-4 bg-muted rounded w-1/2"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <PageHeader />
        <div>
          <p className="text-muted-foreground">Erreur lors du chargement des favoris.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background mb-15">
      <PageHeader />

      <div>
        {/* Onglets */}
        <div className="mb-4 border-b border-border">
          <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets favoris">
            <button
              onClick={() => updateQuery('tab', 'tournaments')}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeTab === 'tournaments'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              Tournois favoris
            </button>
            <button
              onClick={() => updateQuery('tab', 'games')}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeTab === 'games'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              Matchs favoris
            </button>
          </nav>
        </div>

        {/* Contenu des onglets */}
        {activeTab === 'tournaments' && (
          <FavoriteTournamentsList favoriteTournaments={favoriteTournaments} />
        )}

        {activeTab === 'games' && (
          <FavoriteGamesList favoriteGames={favoriteGames} favoriteTournaments={favoriteTournaments} toggleFavoriteGame={toggleFavoriteGame} />
        )}
      </div>

      <BottomNav items={items} pathname={pathname ?? '/'} />
    </div>
  );
}
