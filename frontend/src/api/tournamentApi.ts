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

export async function fetchRounds(tournamentId: string): Promise<Round[]> {
  const response = await fetch(api(`/tournaments/${tournamentId}/rounds`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement des rounds.');
    throw new Error('Erreur de récupération des rounds');
  }
  return await response.json();
}

export async function fetchPairs(tournamentId: string | number, includeByes: boolean = false): Promise<PlayerPair[]> {
  const response = await fetch(api(`/tournaments/${tournamentId}/pairs?includeByes=${includeByes}`));
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

export async function generateDraw(
  tournamentId: string,
  options: { mode: 'auto' } | { mode: 'manual'; rounds?: InitializeDrawRequest['rounds'] }
) {
  const { mode } = options;
  const drawType = mode === 'manual' ? 'manual' : 'auto';

  console.log("generateDraw", drawType);

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
  console.log(`Fetching games for tournament ${tournamentId}, stage ${stage}`);
  const response = await fetch(api(`/tournaments/${tournamentId}/rounds/${stage}/games`));
  if (!response.ok) {
    toast.error('Erreur lors du chargement des matchs.');
    throw new Error('Erreur lors du chargement des matchs.');
  }
  return await response.json();
}