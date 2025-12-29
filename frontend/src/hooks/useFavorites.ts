import { useState, useEffect, useRef } from 'react';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { getFavoriteTournaments, addFavoriteTournament, removeFavoriteTournament, getFavoriteGames, addFavoriteGame, removeFavoriteGame } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import type { Game } from '@/src/types/game';
import { AppError } from '@/src/utils/AppError';

export const useFavorites = (enabled: boolean = true) => {
  const [favoriteTournaments, setFavoriteTournaments] = useState<Tournament[]>([]);
  const [favoriteGames, setFavoriteGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { status } = useSession();
  const hasFetched = useRef(false);
  const router = useRouter();

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
    console.log('[useFavorites] toggleFavoriteTournament called with tournamentId:', tournamentId, 'isFavorite:', isFavorite);

    // Optimistic update: update state immediately
    if (isFavorite) {
      console.log('[useFavorites] Optimistically removing favorite tournament:', tournamentId);
      setFavoriteTournaments(prev => prev.filter(t => t.id !== tournamentId));
    }

    try {
      if (isFavorite) {
        console.log('[useFavorites] Removing favorite tournament:', tournamentId);
        await removeFavoriteTournament(tournamentId);
      } else {
        console.log('[useFavorites] Adding favorite tournament:', tournamentId);
        await addFavoriteTournament(tournamentId);
        await fetchFavorites();
      }
    } catch (err: any) {
      // Revert optimistic update on error
      if (isFavorite) {
        await fetchFavorites();
      } else {
        setFavoriteTournaments(prev => prev.filter(t => t.id !== tournamentId));
      }

      console.log('[useFavorites] Error caught:', err);
      console.log('[useFavorites] Error instanceof AppError:', err instanceof AppError);
      console.log('[useFavorites] Error code:', err?.code);
      console.log('[useFavorites] Error message:', err?.message);

      if (err instanceof AppError && err.code === AppError.UNAUTHORIZED) {
        const currentPath = window.location.pathname + window.location.search;
        console.log('[useFavorites] 401 detected, storing returnUrl:', currentPath);
        localStorage.setItem('authReturnUrl', currentPath);
        console.log('[useFavorites] localStorage after set:', localStorage.getItem('authReturnUrl'));
        router.push('/connexion');
        return;
      }
      setError('Failed to toggle favorite');
    }
  };

  const toggleFavoriteGame = async (gameId: number, isFavorite: boolean, game?: Game) => {
    // Optimistic update: update state immediately
    if (isFavorite) {
      setFavoriteGames(prev => prev.filter(g => Number.parseInt(g.id) !== gameId));
    } else {
      if (game) {
        setFavoriteGames(prev => [...prev, game]);
      }
    }

    try {
      if (isFavorite) {
        await removeFavoriteGame(gameId);
      } else {
        await addFavoriteGame(gameId);
        // If we didn't have the game object, fetch all favorites to get it
        if (!game) {
          await fetchFavorites();
        }
      }
    } catch (err: any) {
      // Revert optimistic update on error
      if (isFavorite) {
        if (game) {
          setFavoriteGames(prev => [...prev, game]);
        } else {
          await fetchFavorites();
        }
      } else {
        setFavoriteGames(prev => prev.filter(g => Number.parseInt(g.id) !== gameId));
      }

      if (err instanceof AppError && err.code === AppError.UNAUTHORIZED) {
        const currentPath = window.location.pathname + window.location.search;
        localStorage.setItem('authReturnUrl', currentPath);
        router.push('/connexion');
        return;
      }
      setError('Failed to toggle favorite');
    }
  };

  useEffect(() => {
    if (enabled && status === 'authenticated' && !hasFetched.current) {
      hasFetched.current = true;
      fetchFavorites();
    }
  }, [enabled, status]);

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
