'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import PageHeader from '@/src/components/ui/PageHeader';
import { useFavorites } from '@/src/hooks/useFavorites';
import FavoriteTournamentCard from '@/src/components/tournament/FavoriteTournamentCard';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { usePathname } from 'next/navigation';
import { Home } from 'lucide-react';
import { FiMoreHorizontal } from 'react-icons/fi';

type TabType = 'tournaments' | 'games';

export default function FavoritesPage() {
  const { favoriteTournaments, favoriteGames, loading, error } = useFavorites();
  const [activeTab, setActiveTab] = useState<TabType>('tournaments');
  const pathname = usePathname();

  const items: BottomNavItem[] = [
    { href: '/', label: 'Accueil', Icon: Home, isActive: (p) => p === '/' },
    { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
  ];

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <PageHeader title="Mes favoris" loading />
        <div className="p-4">
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
        <PageHeader title="Mes favoris" />
        <div className="p-4">
          <p className="text-muted-foreground">Erreur lors du chargement des favoris.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <PageHeader
        title="Mes favoris"
      />

      <div className="p-4">
        {/* Onglets */}
        <div className="mb-4 border-b border-border">
          <nav className="-mb-px flex justify-center gap-2" aria-label="Sous-onglets favoris">
            <button
              onClick={() => setActiveTab('tournaments')}
              className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${
                activeTab === 'tournaments'
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted-foreground hover:text-primary'
              }`}
            >
              Tournois favoris
            </button>
            <button
              onClick={() => setActiveTab('games')}
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
          <div>
            {favoriteTournaments.length === 0 ? (
              <p className="text-muted-foreground">Aucun tournoi favori.</p>
            ) : (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                {favoriteTournaments.map((tournament) => (
                  <FavoriteTournamentCard key={tournament.id} tournament={tournament} />
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'games' && (
          <div>
            {favoriteGames.length === 0 ? (
              <p className="text-muted-foreground">Aucun match favori.</p>
            ) : (
              <div className="space-y-2">
                {favoriteGames.map((game) => (
                  <Link
                    key={game.id}
                    href={`/tournament/${game.tournamentId}/games/${game.gameId}`}
                    className="block p-3 bg-card rounded-lg border hover:bg-accent transition-colors"
                  >
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="font-medium">
                          {game.teamA?.map(p => p.name).join(' & ')} vs {game.teamB?.map(p => p.name).join(' & ')}
                        </p>
                        <p className="text-sm text-muted-foreground">Court {game.court} - {game.scheduledTime}</p>
                      </div>
                      {game.finished && (
                        <div className="text-sm font-medium">
                          {game.score ? `${game.score.teamAScore} - ${game.score.teamBScore}` : 'Terminé'}
                        </div>
                      )}
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <BottomNav items={items} pathname={pathname} />
    </div>
  );
}
