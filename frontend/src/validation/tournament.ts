// src/validation/tournament.ts
import { z } from 'zod';

// Enum for tournament format (aligned with backend)
export const TournamentFormatEnum = z.enum(['KNOCKOUT', 'GROUPS_KO']);

// Helpers
const emptyToNull = (v: unknown) => (v === '' || v === undefined ? null : v);
const trimThenEmptyToNull = (v: unknown) => {
  if (typeof v === 'string') {
    const s = v.trim();
    return s === '' ? null : s;
  }
  return v === '' || v === undefined ? null : v;
};
const intFromInput = z.coerce.number().int();
const num = (v: number | null | undefined, fallback = 0) =>
  typeof v === 'number' && !Number.isNaN(v) ? v : fallback;

// Frontend form validation schema (keeps legacy fields for the form only)
export const TournamentFormSchema = z
  .object({
    id: z.number().int().nullable().optional().default(null),
    name: z.string().trim().min(1, 'Le tournoi doit avoir un nom.').default(''),

    description: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
    city: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
    club: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
    gender: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
    level: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),

    tournamentFormat: TournamentFormatEnum.default('KNOCKOUT'),

    nbSeeds: z.preprocess(emptyToNull, intFromInput.nullable()).default(16),

    // Dates are strings from <input type="date"> (ISO yyyy-MM-dd)
    startDate: z.preprocess(emptyToNull, z.string().nullable()).default(null),
    endDate: z.preprocess(emptyToNull, z.string().nullable()).default(null),

    nbMaxPairs: z.preprocess(emptyToNull, intFromInput.nullable()).default(32),

    // Group-only (kept in the form, not sent at root in payload)
    nbPools: z.preprocess(emptyToNull, intFromInput.nullable()).default(4),
    nbPairsPerPool: z.preprocess(emptyToNull, intFromInput.nullable()).default(4),
    nbQualifiedByPool: z.preprocess(emptyToNull, intFromInput.nullable()).default(2),
  })
  .superRefine((data, ctx) => {
    const format = data.tournamentFormat;
    const nbSeeds = num(data.nbSeeds);
    const nbMaxPairs = num(data.nbMaxPairs);
    const nbPools = num(data.nbPools);
    const nbPairsPerPool = num(data.nbPairsPerPool);

    if (format === 'KNOCKOUT') {
      if (nbMaxPairs < 4 || nbMaxPairs > 128) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `En élimination directe, le nombre d'équipes maximum doit être entre 4 et 128 (actuellement ${nbMaxPairs}).`,
          path: ['nbMaxPairs'],
        });
      }
      const maxSeeds = Math.floor(nbMaxPairs / 2);
      if (nbSeeds > maxSeeds) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `En élimination directe, les têtes de série (${nbSeeds}) ne peuvent pas dépasser la moitié des équipes (${nbMaxPairs} → max ${maxSeeds}).`,
          path: ['nbSeeds'],
        });
      }
    }

    if (format === 'GROUPS_KO') {
      if (nbPairsPerPool < 3) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `En poules, il faut au moins 3 équipes par poule (actuellement ${nbPairsPerPool}).`,
          path: ['nbPairsPerPool'],
        });
      }
      const maxSeeds = nbPools * nbPairsPerPool;
      if (nbSeeds > maxSeeds) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: `En poules, les têtes de série (${nbSeeds}) ne peuvent pas dépasser le total d'équipes (${nbPools}×${nbPairsPerPool} = ${maxSeeds}).`,
          path: ['nbSeeds'],
        });
      }
    }

    // Dates ordering check (ISO yyyy-MM-dd strings compare lexicographically)
    if (data.startDate && data.endDate && data.startDate > data.endDate) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'La date de fin doit être postérieure ou égale à la date de début.',
        path: ['endDate'],
      });
    }
  });

export type TournamentFormData = z.input<typeof TournamentFormSchema>;   // état du formulaire (avant parse)
export type ParsedTournamentForm = z.infer<typeof TournamentFormSchema>;

// Payload with formatConfig (aligned with backend)
export type KnockoutConfigPayload = { mainDrawSize: number; nbSeeds: number };
export type GroupsKoConfigPayload = { nbPools: number; nbPairsPerPool: number; nbQualifiedByPool: number; mainDrawSize: number; nbSeeds: number };

export type TournamentPayload = {
  id: number | null;
  name: string;
  description: string | null;
  city: string | null;
  club: string | null;
  gender: string | null;
  level: string | null;
  tournamentFormat: 'KNOCKOUT' | 'GROUPS_KO';
  nbSeeds: number | null;
  startDate: string | null;
  endDate: string | null;
  nbMaxPairs: number | null;
  formatConfig: KnockoutConfigPayload | GroupsKoConfigPayload;
};

// Helper to compute a power-of-two main draw size (<= nbMaxPairs)
const greatestPowerOfTwo = (n: number) => {
  if (n < 2) return 1;
  return 1 << Math.floor(Math.log2(n));
};

export function buildTournamentPayload(form: ParsedTournamentForm): TournamentPayload {
  const base = {
    id: form.id ?? null,
    name: form.name,
    description: form.description ?? null,
    city: form.city ?? null,
    club: form.club ?? null,
    gender: form.gender ?? null,
    level: form.level ?? null,
    tournamentFormat: form.tournamentFormat,
    nbSeeds: form.nbSeeds ?? null,
    startDate: form.startDate ?? null,
    endDate: form.endDate ?? null,
    nbMaxPairs: form.nbMaxPairs ?? null,
  } as Omit<TournamentPayload, 'formatConfig'>;

  if (form.tournamentFormat === 'KNOCKOUT') {
    const md = greatestPowerOfTwo(num(form.nbMaxPairs, 0));
    const cfg: KnockoutConfigPayload = {
      mainDrawSize: md,
      nbSeeds: num(form.nbSeeds, 0),
    };
    return { ...base, formatConfig: cfg };
  }

  // GROUPS_KO
  const nbPools = num(form.nbPools, 0);
  const nbQualifiedByPool = num(form.nbQualifiedByPool, 0);
  const cfg: GroupsKoConfigPayload = {
    nbPools,
    nbPairsPerPool: num(form.nbPairsPerPool, 0),
    nbQualifiedByPool,
    mainDrawSize: nbPools * nbQualifiedByPool,
    nbSeeds: num(form.nbSeeds, 0),
  };
  return { ...base, formatConfig: cfg };
}
