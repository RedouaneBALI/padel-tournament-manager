import type { Round } from '@/src/types/round';
import type { PlayerPair } from '@/src/types/playerPair';
import type { Tournament } from '@/src/types/tournament';
import type { MatchFormat } from '@/src/types/matchFormat';
import type { Score } from '@/src/types/score';

const BASE_URL = 'http://localhost:8080/tournaments';

export async function fetchTournament(tournamentId : string) : Promise<Tournament> {
  const response = await fetch(`${BASE_URL}/${tournamentId}`);
  if (!response.ok) throw new Error('Erreur de récupération du tournoi');
  return await response.json();
}

export async function fetchRounds(tournamentId: string): Promise<Round[]> {
  const response = await fetch(`${BASE_URL}/${tournamentId}/rounds`);
  if (!response.ok) throw new Error('Erreur de récupération des rounds');
  return await response.json();
}

export async function fetchPairs(tournamentId: string): Promise<PlayerPair[]>{
  const response = await fetch(`${BASE_URL}/${tournamentId}/pairs`);
  if (!response.ok) throw new Error('Erreur de récupération des PlayerPair');
  return await response.json();
}

export async function fetchMatchFormat(tournamentId: string, currentStage: string): Promise<MatchFormat>{
  const response = await fetch(`${BASE_URL}/${tournamentId}/rounds/${currentStage}/match-format`);
  if (!response.ok) throw new Error('Erreur de récupération du MatchFormat');
  return await response.json();
}

export async function updateGameDetails(tournamentId: string, gameId: string, scorePayload: Score, court: string, scheduledTime: string) {
  const response = await fetch(`${BASE_URL}/${tournamentId}/games/${gameId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
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

export async function updateMatchFormat(tournamentId: string, stage: string, matchFormat: MatchFormat) {
  const response = await fetch(`${BASE_URL}/${tournamentId}/rounds/${stage}/match-format`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(matchFormat),
  });

  if (!response.ok) {
    throw new Error('Erreur lors de la mise à jour du MatchFormat');
  }

  return await response.json();
}

export async function createTournament(newTournament: Tournament): Promise<Tournament> {
  const response = await fetch(`${BASE_URL}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(newTournament),
  });

  if (!response.ok) {
    throw new Error('Erreur lors de la création du tournoi');
  }

  return await response.json();
}

export async function updateTournament(tournamentId: string, updatedTournament: Tournament) {
  const response = await fetch(`${BASE_URL}/${tournamentId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(updatedTournament),
  });

  if (!response.ok) {
    throw new Error('Erreur lors de la mise à jour du tournoi');
  }

  return await response.json();
}

export async function savePlayerPairs(tournamentId: string, pairs: PlayerPair[]) {
  const response = await fetch(`${BASE_URL}/${tournamentId}/pairs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(pairs),
  });

  if (!response.ok) {
    throw new Error("Erreur lors de l'enregistrement des joueurs.");
  }

  return await response.json();
}

export async function generateDraw(tournamentId: string, manual: boolean) {
  const response = await fetch(`${BASE_URL}/${tournamentId}/draw?manual=${manual}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    throw new Error('Erreur lors de la génération du tirage');
  }

  return await response.json();
}
