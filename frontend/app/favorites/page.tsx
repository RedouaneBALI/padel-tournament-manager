'use client';

import React from 'react';
import Link from 'next/link';
import PageHeader from '@/src/components/ui/PageHeader';
import { useFavorites } from '@/src/hooks/useFavorites';
import { FaStar, FaRegStar } from 'react-icons/fa';

export default function FavoritesPage() {
  const { favoriteTournaments, favoriteGames, loading, error } = useFavorites();

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
      <PageHeader title="Mes favoris" />

      <div className="p-4 space-y-6">
        {/* Tournois favoris */}
        <div>
          <h2 className="text-lg font-semibold mb-2 flex items-center gap-2">
            <FaStar className="text-yellow-400" />
            Tournois favoris
          </h2>
          {favoriteTournaments.length === 0 ? (
            <p className="text-muted-foreground">Aucun tournoi favori.</p>
          ) : (
            <div className="space-y-2">
              {favoriteTournaments.map((tournament) => (
                <Link
                  key={tournament.id}
                  href={`/tournament/${tournament.id}`}
                  className="block p-3 bg-card rounded-lg border hover:bg-accent transition-colors"
                >
                  <h3 className="font-medium">{tournament.name}</h3>
                  <p className="text-sm text-muted-foreground">{tournament.city}, {tournament.club}</p>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Matchs favoris */}
        <div>
          <h2 className="text-lg font-semibold mb-2 flex items-center gap-2">
            <FaRegStar className="text-yellow-400" />
            Matchs favoris
          </h2>
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
      </div>
    </div>
  );
}
