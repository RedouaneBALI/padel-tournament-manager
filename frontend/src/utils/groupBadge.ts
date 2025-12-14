// utils/groupBadge.ts

export const GROUP_LETTERS = ['A','B','C','D','E','F','G','H'] as const;
export type GroupLetter = (typeof GROUP_LETTERS)[number];

/** Extrait une lettre de groupe A..H (ou mappe 1..8 -> A..H). */
export function normalizeGroup(poolName?: string): GroupLetter | null {
  if (!poolName) return null;
  const s = poolName.trim().toUpperCase();

  // Cherche une lettre A..H isolée
  const mLetter = s.match(/\b([A-H])\b/);
  if (mLetter) return mLetter[1] as GroupLetter;

  // Sinon chiffre 1..8 -> A..H
  const mNum = s.match(/\b([1-8])\b/);
  if (mNum) return GROUP_LETTERS[parseInt(mNum[1], 10) - 1];

  // Dernier recours : unique char A..H
  if (s.length === 1 && GROUP_LETTERS.includes(s as GroupLetter)) return s as GroupLetter;

  return null;
}

/** Classes tailwind pour le badge selon la poule (clair & dark). */
export function groupBadgeClasses(group: GroupLetter | null): string {
  switch (group) {
    case 'A': return 'bg-sky-500 text-sky-100';
    case 'B': return 'bg-emerald-500 text-emerald-100';
    case 'C': return 'bg-amber-500 text-amber-100';
    case 'D': return 'bg-violet-500 text-violet-100';
    case 'E': return 'bg-rose-500 text-rose-100';
    case 'F': return 'bg-cyan-500 text-cyan-100';
    case 'G': return 'bg-lime-500 text-lime-100';
    case 'H': return 'bg-indigo-500 text-indigo-100';
    default:  return 'bg-muted text-foreground';
  }
}

/** Formatte "Poule X" en gardant une sortie stable même si le nom est "Groupe 1". */
export function formatGroupLabel(poolName?: string): string {
  const g = normalizeGroup(poolName);
  return `Poule ${g ?? (poolName ?? '')}`.trim();
}