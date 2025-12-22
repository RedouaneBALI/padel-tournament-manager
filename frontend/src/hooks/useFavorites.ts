import { useState, useEffect } from 'react';
import { useSession } from 'next-auth/react';
import { getFavoriteTournaments, addFavoriteTournament, removeFavoriteTournament, getFavoriteGames, addFavoriteGame, removeFavoriteGame } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import type { Game } from '@/src/types/game';

export const useFavorites = () => {
  const [favoriteTournaments, setFavoriteTournaments] = useState<Tournament[]>([]);
  const [favoriteGames, setFavoriteGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { status } = useSession();

  const fetchFavorites = async () => {
    if (status !== 'authenticated') return;
    setLoading(true);
    setError(null);
    try {
      const [tournaments, games] = await Promise.all([
        getFavoriteTournaments(),
        getFavoriteGames(),
      ]);
      setFavoriteTournaments(tournaments);
      setFavoriteGames(games);
    } catch (err) {
      console.error('Error fetching favorites:', err);
      setError('Failed to fetch favorites');
      setFavoriteTournaments([]);
      setFavoriteGames([]);
    } finally {
      setLoading(false);
    }
  };

  const toggleFavoriteTournament = async (tournamentId: number, isFavorite: boolean) => {
    if (status !== 'authenticated') return;
    try {
      if (isFavorite) {
        await removeFavoriteTournament(tournamentId);
        setFavoriteTournaments(prev => prev.filter(t => t.id !== tournamentId));
      } else {
        await addFavoriteTournament(tournamentId);
        // Note: Since we don't have the full tournament data, we might need to fetch or assume it's added
        // For now, we'll refetch
        await fetchFavorites();
      }
    } catch (err) {
      console.error('Error toggling favorite tournament:', err);
      setError('Failed to toggle favorite');
    }
  };

  const toggleFavoriteGame = async (gameId: number, isFavorite: boolean) => {
    if (status !== 'authenticated') return;
    try {
      if (isFavorite) {
        await removeFavoriteGame(gameId);
        setFavoriteGames(prev => prev.filter(g => parseInt(g.id) !== gameId));
      } else {
        await addFavoriteGame(gameId);
        // Refetch to get the full game data
        await fetchFavorites();
      }
    } catch (err) {
      console.error('Error toggling favorite game:', err);
      setError('Failed to toggle favorite');
    }
  };

  useEffect(() => {
    fetchFavorites();
  }, [status]);

  return {
    favoriteTournaments,
    favoriteGames,
    loading,
    error,
    toggleFavoriteTournament,
    toggleFavoriteGame,
    refetch: fetchFavorites,
  };
};
