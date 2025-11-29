// src/api/tournamentApi.ts
// API shape for Game coming from backend (snake_case via @JsonNaming)
import { toast } from 'react-toastify';
import type { Round } from '@/src/types/round';
import type { PlayerPair } from '@/src/types/playerPair';
import type { Tournament } from '@/src/types/tournament';
import type { MatchFormat } from '@/src/types/matchFormat';
import type { Score } from '@/src/types/score';
import { fetchWithAuth } from "./fetchWithAuth";
import type { InitializeDrawRequest } from '@/src/types/api/InitializeDrawRequest';
import type { Game } from '@/src/types/game';

// Utiliser directement l'URL du backend depuis les variables d'environnement
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

const api = (path: string) => {
  // Si le path commence par /api/auth, c'est NextAuth - on garde le chemin relatif
  if (path.startsWith('/auth')) {
    return `/api${path}`;
  }
  // Sinon, on utilise l'URL complète du backend
  return `${API_BASE_URL}${path}`;
};

export async function fetchTournament(tournamentId: string): Promise<Tournament> {
  const res = await fetch(api(`/tournaments/${tournamentId}`), { method: 'GET' });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    toast.error(`Erreur lors de la récupération du tournoi (${res.status})`);
    throw new Error(`HTTP_${res.status} ${text}`);
  }
  return await res.json();
}

export async function fetchTournamentAdmin(tournamentId: string): Promise<Tournament> {
  const res = await fetchWithAuth(api(`/tournaments/${tournamentId}`), {
    method: "GET",
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    toast.error(`Erreur lors de la récupération du tournoi (${res.status})`);
    throw new Error(`HTTP_${res.status} ${text}`);
  }

  return await res.json();
}

export async function fetchMyTournaments(scope: 'mine' | 'all' = 'mine'): Promise<Tournament[]> {
  const qs = scope && scope !== 'mine' ? `?scope=${encodeURIComponent(scope)}` : '';
  const res = await fetchWithAuth(api(`/admin/tournaments${qs}`), { method: 'GET' });

  if (res.status === 401) {
    throw new Error('UNAUTHORIZED');
  }
  if (res.status === 403) {
    throw new Error('FORBIDDEN');
  }
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    toast.error('Erreur lors du chargement de vos tournois.');
    throw new Error(`HTTP_${res.status} ${text}`);
  }

  return await res.json();
}

export async function fetchMyGames(scope: 'mine' | 'all' = 'mine') {
  const qs = scope && scope !== 'mine' ? `?scope=${encodeURIComponent(scope)}` : '';
  const res = await fetchWithAuth(api(`/admin/games${qs}`), { method: 'GET' });

  if (res.status === 401) throw new Error('UNAUTHORIZED');
  if (res.status === 403) throw new Error('FORBIDDEN');
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    toast.error('Erreur lors du chargement de vos matchs.');
    throw new Error(`HTTP_${res.status} ${text}`);
  }

  return await res.json();
}

export async function fetchRounds(tournamentId: string): Promise<Round[]> {
  const response = await fetch(api(`/tournaments/${tournamentId}/rounds`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement des rounds.');
    throw new Error('Erreur de récupération des rounds');
  }
  return await response.json();
}

export async function fetchPairs(tournamentId: string | number, includeByes: boolean = false, includeQualified: boolean = false): Promise<PlayerPair[]> {
  const response = await fetch(api(`/tournaments/${tournamentId}/pairs?includeByes=${includeByes}&includeQualified=${includeQualified}`));
  if (!response.ok) {
    throw new Error('Erreur de récupération des PlayerPair');
  }
  return await response.json();
}

export async function fetchMatchFormat(tournamentId: string, currentStage: string): Promise<MatchFormat> {
  const response = await fetch(api(`/tournaments/${tournamentId}/rounds/${currentStage}/match-format`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement du format.');
    throw new Error('Erreur de récupération du MatchFormat');
  }
  return await response.json();
}

export async function updateGameDetails(tournamentId: string, gameId: string, scorePayload: Score, court: string, scheduledTime: string) {
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/games/${gameId}`), {
    method: 'PUT',
    body: JSON.stringify({
      score: scorePayload,
      court,
      scheduledTime,
    }),
  });

  if (!response.ok) {
    throw new Error('Erreur lors de la sauvegarde');
  }

  return await response.json();
}

export async function updateMatchFormat(tournamentId: string, stage: string, matchFormat: MatchFormat, showToast: boolean = true) {
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/rounds/${stage}/match-format`), {
    method: 'PUT',
    body: JSON.stringify(matchFormat),
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    if (showToast) {
      toast.error('Erreur lors de la mise à jour des rounds.');
    }
    throw new Error(`Erreur lors de la mise à jour du MatchFormat (${response.status}) ${text}`);
  }

  if (showToast) {
    toast.success('Format du match enregistré avec succès.');
  }
  return await response.json();
}

/**
 * Crée un tournoi.
 * @param newTournament payload
 * @param idToken id_token Google (JWT) à mettre dans Authorization (Bearer)
 */
export async function createTournament(payload: Tournament) {
  const res = await fetchWithAuth(api(`/admin/tournaments`), {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    toast.error("Erreur lors de la création du tournoi : " + text);
    throw new Error(`Erreur création (${res.status}) ${text}`);
  }
  toast.success('Tournoi créé !');
  return await res.json();
}

export async function deleteTournament(tournamentId: string | number): Promise<void> {
  const res = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}`), {
    method: 'DELETE',
  });

  if (res.status === 401) throw new Error('UNAUTHORIZED');
  if (res.status === 403) throw new Error('FORBIDDEN');

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    toast.error("Erreur lors de la suppression du tournoi.");
    throw new Error(`HTTP_${res.status} ${text}`);
  }
  // 204 No Content attendu — rien à retourner
}

export async function updateTournament(tournamentId: string, updatedTournament: Tournament) {
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}`), {
    method: 'PUT',
    body: JSON.stringify(updatedTournament),
  });

  if (!response.ok) {
    toast.error('Erreur lors de la mise à jour.');
    throw new Error('Erreur lors de la mise à jour du tournoi');
  }

  toast.success('Tournoi mis à jour !');
  return await response.json();
}

export async function savePlayerPairs(tournamentId: string, pairs: PlayerPair[]) {
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/pairs`), {
    method: 'POST',
    body: JSON.stringify(pairs),
  });

  if (!response.ok) {
    toast.error("Erreur lors de l'enregistrement des joueurs.");
    throw new Error("Erreur lors de l'enregistrement des joueurs.");
  }

  toast.success('Joueurs ajoutés avec succès !');
  return await response.json();
}

/**
 * Reorders the complete list of player pairs for a tournament.
 * Useful for manual draw management and seeding adjustments.
 * Only the owner or super admins can reorder pairs.
 *
 * @param tournamentId the tournament ID
 * @param pairIds ordered list of pair IDs (must contain all existing pair IDs)
 */
export async function reorderPlayerPairs(tournamentId: string, pairIds: number[]) {
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/pairs/reorder`), {
    method: 'PUT',
    body: JSON.stringify(pairIds),
  });

  if (!response.ok) {
    toast.error("Erreur lors de la réorganisation de l'ordre des joueurs.");
    throw new Error("Erreur lors de la réorganisation de l'ordre des joueurs.");
  }

  // Pas de toast de succès pour ne pas polluer l'interface lors des drag & drop
}

export async function generateDraw(
  tournamentId: string,
  options: { mode: 'auto' } | { mode: 'manual'; rounds?: InitializeDrawRequest['rounds'] }
) {
  const { mode } = options;
  const drawType = mode === 'manual' ? 'manual' : 'auto';

  const requestOptions: RequestInit = {
    method: 'POST',
  };

  // Si mode manuel avec des rounds, on les envoie dans le body
  if (mode === 'manual' && 'rounds' in options && options.rounds) {
    requestOptions.headers = { 'Content-Type': 'application/json' };
    requestOptions.body = JSON.stringify(options.rounds);
  }

  const response = await fetchWithAuth(
    api(`/admin/tournaments/${tournamentId}/draw/${drawType}`),
    requestOptions
  );

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error("Tirage déjà effectué ou erreur serveur.");
    throw new Error(`Erreur lors de la génération du tirage (${response.status}) ${text}`);
  }

  toast.success('Tirage généré !');
  return await response.json();
}

export async function initializeDraw(tournamentId: string, payload: InitializeDrawRequest) {
  var json = JSON.stringify(payload);
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/draw/initialize`), {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error("Erreur lors de l'initialisation manuelle du tirage.");
    throw new Error(`Erreur lors de l'initialisation du tirage (${response.status}) ${text}`);
  }

  toast.success('Tirage initialisé !');
  return await response.json();
}

export async function updatePlayerPair(
  tournamentId: string | number,
  pairId: string | number,
  payload: { player1Name?: string; player2Name?: string; seed?: number }
): Promise<void> {
  const res = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/pairs/${pairId}`), {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

  if (res.status === 401) throw new Error('UNAUTHORIZED');
  if (res.status === 403) throw new Error('FORBIDDEN');
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP_${res.status} ${text}`);
  }
  // 200 OK with no body expected
}

export async function fetchGamesByStage(tournamentId: string, stage: string) {
  const response = await fetch(api(`/tournaments/${tournamentId}/rounds/${stage}/games`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement des matchs.');
    throw new Error('Erreur lors du chargement des matchs.');
  }
  return await response.json();
}

export async function fetchGame(tournamentId: string, gameId: string) {
  const response = await fetch(api(`/tournaments/${tournamentId}/games/${gameId}`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement du match.');
    throw new Error('Erreur lors du chargement du match.');
  }
  return await response.json();
}

/**
 * Creates a standalone game (independent of any tournament).
 * @param teamA First team (PlayerPair with player1Name and player2Name)
 * @param teamB Second team (PlayerPair with player1Name and player2Name)
 * @param format Match format configuration
 */
export async function createStandaloneGame(
  teamA: PlayerPair,
  teamB: PlayerPair,
  format: MatchFormat
): Promise<Game> {
  const payload = {
    teamA: {
      player1Name: teamA.player1Name,
      player2Name: teamA.player2Name,
    },
    teamB: {
      player1Name: teamB.player1Name,
      player2Name: teamB.player2Name,
    },
    format,
  };

  const res = await fetchWithAuth(api(`/admin/games`), {
    method: 'POST',
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    toast.error('Erreur lors de la création du match : ' + text);
    throw new Error(`Erreur création match (${res.status}) ${text}`);
  }

  toast.success('Match créé avec succès !');
  return await res.json();
}

/**
 * Fetches a standalone game by its ID (independent of any tournament).
 * @param gameId The ID of the standalone game
 */
export async function fetchStandaloneGame(gameId: string): Promise<Game> {
  const response = await fetchWithAuth(api(`/games/${gameId}`), {
    method: 'GET',
  });
  if (!response.ok) {
    toast.error('Erreur lors du chargement du match.');
    throw new Error('Erreur lors du chargement du match.');
  }
  return await response.json();
}

/**
 * Updates a standalone game (score, court, scheduled time).
 * @param gameId The ID of the standalone game
 * @param scorePayload The score data
 * @param court The court name
 * @param scheduledTime The scheduled time
 */
export async function updateStandaloneGame(gameId: string, scorePayload: Score, court: string, scheduledTime: string) {
  const response = await fetchWithAuth(api(`/admin/games/${gameId}`), {
    method: 'PUT',
    body: JSON.stringify({
      score: scorePayload,
      court,
      scheduledTime,
    }),
  });

  if (!response.ok) {
    throw new Error('Erreur lors de la sauvegarde');
  }

  return await response.json();
}

export async function fetchActiveTournaments() {
  const response = await fetch(api(`/tournaments/active`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement des tournois à la une.');
    throw new Error('Erreur lors du chargement des tournois à la une.');
  }
  return await response.json();
}

/**
 * Incrémente ou décrémente le point en cours pour une équipe dans un match de tournoi (admin).
 */
export async function updateGamePoint(tournamentId: string | number, gameId: string | number, teamSide: string, increment: boolean) {
  const response = await fetchWithAuth(api(`/admin/tournaments/${tournamentId}/games/${gameId}/game-point`), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ teamSide, increment }),
  });
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error('Erreur lors de la mise à jour du point.');
    throw new Error(`Erreur updateGamePoint (${response.status}) ${text}`);
  }
  return await response.json();
}
