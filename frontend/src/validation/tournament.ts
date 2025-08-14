import { z } from 'zod';

// Enum for tournament format
export const TournamentFormatEnum = z.enum(['KNOCKOUT', 'GROUP_STAGE']);

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

// Frontend form validation schema
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
    nbPools: z.preprocess(emptyToNull, intFromInput.nullable()).default(4),
    nbPairsPerPool: z.preprocess(emptyToNull, intFromInput.nullable()).default(4),
    nbQualifiedByPool: z.preprocess(emptyToNull, intFromInput.nullable()).default(2),
  })
  .superRefine((data, ctx) => {
    // Helper to safely read numbers that can be null/undefined
    const num = (v: number | null | undefined, fallback = 0) =>
      typeof v === 'number' ? v : fallback;

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

    if (format === 'GROUP_STAGE') {
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
export type TournamentPayload  = z.output<typeof TournamentFormSchema>;  // données propres (après parse)
