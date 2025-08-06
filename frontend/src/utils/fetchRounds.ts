import type { Round } from '@/src/types/round';
import type { Round } from '@/src/types/PlayerPair';
import type { Round } from '@/src/types/Tournament';
import type { Round } from '@/src/types/MatchFormat';

export async function fetchTournament(tournamentId : string) : Promise<Tournament> {
  const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}`);
  if (!response.ok) throw new Error('Erreur de récupération du tournoi');
  return await response.json();
}

export async function fetchRounds(tournamentId: string): Promise<Round[]> {
  const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/rounds`);
  if (!response.ok) throw new Error('Erreur de récupération des rounds');
  return await response.json();
}

export async function fetchPairs(tournamentId: string): Promise<PlayerPair[]>{
  const response = await fetch(`http://localhost:8080/tournaments/${id}/pairs`);
  if (!response.ok) throw new Error('Erreur de récupération des PlayerPair');
  return await response.json();
}

export async function fetchMatchFormat(tournamentId: string, currentStage, string): Promise<MatchFormat>{
  const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/rounds/${currentStage}/match-format`);
  if (!response.ok) throw new Error('Erreur de récupération du MatchFormat');
  return await response.json();
}

// @todo remplace in class with these methods
// @todo add here post/put methods