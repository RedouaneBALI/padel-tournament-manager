import { Player } from '../PlayerDetailModal'

export function getUniqueNationalities(players: Player[]): string[] {
  const nationalities = players.map(p => p.nationality).filter(Boolean)

  const counts = nationalities.reduce<Record<string, number>>((acc, val) => {
    acc[val] = (acc[val] || 0) + 1
    return acc
  }, {})

  return Array.from(new Set(nationalities)).sort((a, b) => (counts[b] || 0) - (counts[a] || 0))
}

export function getUniqueClubs(players: Player[]): string[] {
  const clubs = players.map(p => p.club).filter(Boolean)

  const counts = clubs.reduce<Record<string, number>>((acc, val) => {
    acc[val] = (acc[val] || 0) + 1
    return acc
  }, {})

  return Array.from(new Set(clubs)).sort((a, b) => (counts[b] || 0) - (counts[a] || 0))
}

function normalizeString(str: string | undefined): string {
  if (!str) return '';
  return str
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9-]/g, '');
}

export function generatePlayerId(player: Player): string {
  const namePart = normalizeString(player.name);
  const birthYearPart = player.birth_year.toString();
  const clubPart = normalizeString(player.club);
  const nationalityPart = normalizeString(player.nationality);

  return `${namePart}-${birthYearPart}-${clubPart}-${nationalityPart}`;
}