import type { Round } from '@/src/types/round';

export async function fetchRounds(tournamentId: string): Promise<Round[]> {
  const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/rounds`);
  if (!response.ok) throw new Error('Erreur de récupération des rounds');
  return await response.json();
}