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
import type { User } from '@/src/types/user';
import type { VoteSummary, VotePayload } from '@/src/types/vote';

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

const getSessionId = (): string => {
  let sessionId = localStorage.getItem('sessionId');
  if (!sessionId) {
    sessionId = crypto.randomUUID();
    localStorage.setItem('sessionId', sessionId);
  }
  return sessionId;
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

  if (res.status === 401) {
    throw new Error('UNAUTHORIZED');
  }
  if (res.status === 403) {
    throw new Error('FORBIDDEN');
  }
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
  rounds: InitializeDrawRequest['rounds']
) {
  const requestOptions: RequestInit = {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(rounds),
  };

  const response = await fetchWithAuth(
    api(`/admin/tournaments/${tournamentId}/draw/manual`),
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
export async function incrementGamePoint(tournamentId: string | number, gameId: string | number, teamSide: string) {
  return await patchGamePointEndpoint(
    `/admin/tournaments/${tournamentId}/games/${gameId}/game-point`,
    teamSide,
    'Erreur lors de l\'incrémentation du point.'
  );
}

/**
 * Undoes the last game point for a team (admin).
 * @param tournamentId
 * @param gameId
 */
export async function undoGamePoint(tournamentId: string | number, gameId: string | number) {
  return await patchGamePointEndpoint(
    `/admin/tournaments/${tournamentId}/games/${gameId}/undo-game-point`,
    undefined,
    'Erreur lors de l\'annulation du point.'
  );
}

/**
 * Fetches the current user's profile.
 */
export async function fetchUserProfile(): Promise<User> {
  const response = await fetchWithAuth(api('/user/profile'), {
    method: 'GET',
  });

  if (response.status === 401) {
    throw new Error('UNAUTHORIZED');
  }
  if (response.status === 403) {
    throw new Error('FORBIDDEN');
  }
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error('Erreur lors du chargement du profil.');
    throw new Error(`Erreur lors du chargement du profil (${response.status}) ${text}`);
  }

  return await response.json();
}

/**
 * Updates the current user's profile.
 * @param payload Partial update payload excluding id and email
 */
export async function updateUserProfile(payload: Partial<Omit<User, 'id' | 'email'>>): Promise<User> {
  const response = await fetchWithAuth(api('/user/profile'), {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error('Erreur lors de la mise à jour du profil.');
    throw new Error(`Erreur lors du mise à jour du profil (${response.status}) ${text}`);
  }

  toast.success('Profil mis à jour avec succès !');
  return await response.json();
}

export async function submitVote(gameId: string, teamSide: 'TEAM_A' | 'TEAM_B'): Promise<VoteSummary> {
  const url = api(`/games/${gameId}/votes`);
  const response = await fetchWithAuth(url, {
    method: 'POST',
    headers: { 'X-Session-Id': getSessionId() },
    body: JSON.stringify({ teamSide }),
  });

  if (response.status === 409) {
    toast.error('Vous avez déjà voté pour ce match.');
    throw new Error('ALREADY_VOTED');
  }

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error('Erreur lors du vote.');
    throw new Error(`Erreur lors du vote (${response.status}) ${text}`);
  }

  return await response.json();
}

export async function fetchVoteSummary(gameId: string): Promise<VoteSummary> {
  const response = await fetchWithAuth(api(`/games/${gameId}/votes`), {
    method: 'GET',
    headers: { 'X-Session-Id': getSessionId() },
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error('Erreur lors du chargement des votes.');
    throw new Error(`Erreur lors du chargement des votes (${response.status}) ${text}`);
  }

  return await response.json();
}

// Internal helper to avoid code duplication
async function patchGamePointEndpoint(endpoint: string, teamSide: string | undefined, errorMsg: string) {
  const url = teamSide !== undefined ? api(endpoint) + `?teamSide=${encodeURIComponent(teamSide)}` : api(endpoint);
  const response = await fetchWithAuth(url, {
    method: 'PATCH',
  });
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    toast.error(errorMsg);
    throw new Error(`${errorMsg} (${response.status}) ${text}`);
  }
  return await response.json();
}
