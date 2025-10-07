// src/validation/tournament.ts
import { z } from 'zod';

// Helpers
const emptyToNull = (v: unknown) => (v === '' || v === undefined ? null : v);
const trimThenEmptyToNull = (v: unknown) => {
  if (typeof v === 'string') {
    const s = v.trim();
    return s === '' ? null : s;
  }
  return v === '' || v === undefined ? null : v;
};
const num = (v: number | null | undefined, fallback = 0) =>
  typeof v === 'number' && !Number.isNaN(v) ? v : fallback;

export const TournamentFormatConfigSchema = z.object({
  format: z.enum(['KNOCKOUT', 'GROUPS_KO', 'QUALIF_KO']),
  mainDrawSize: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(32),
  nbSeeds: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(null),
  drawMode: z.preprocess((v) => (v === '' || v === undefined ? 'SEEDED' : v), z.enum(['SEEDED', 'MANUAL'])).default('SEEDED'),
  nbPools: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(4),
  nbPairsPerPool: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(4),
  nbQualifiedByPool: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(2),
  preQualDrawSize: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(16),
  nbQualifiers: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return null;
    const n = Number(v);
    return isNaN(n) ? null : Math.trunc(n);
  }, z.number().nullable()).default(2),
  nbSeedsQualify: z.preprocess((v) => {
    if (v === '' || v === undefined || v === null) return 0;
    const n = Number(v);
    return isNaN(n) ? 0 : Math.trunc(n);
  }, z.number().nullable()).default(0),
});

export const TournamentFormSchema = z.object({
  id: z.number().int().nullable().optional().default(null),
  name: z.string()
    .trim()
    .min(1, 'Le nom du tournoi est requis.')
    .max(200, 'Le nom du tournoi ne doit pas dépasser 200 caractères.')
    .default(''),
  description: z.preprocess(trimThenEmptyToNull, z.string()
    .max(1000, 'La description ne doit pas dépasser 1000 caractères.')
    .nullable()).default(null),
  city: z.preprocess(trimThenEmptyToNull, z.string()
    .max(100, 'Le nom de la ville ne doit pas dépasser 100 caractères.')
    .nullable()).default(null),
  club: z.preprocess(trimThenEmptyToNull, z.string()
    .max(200, 'Le nom du club ne doit pas dépasser 200 caractères.')
    .nullable()).default(null),
  gender: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  level: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  startDate: z.preprocess(emptyToNull, z.string().nullable()).default(null),
  endDate: z.preprocess(emptyToNull, z.string().nullable()).default(null),
  config: TournamentFormatConfigSchema,
})
.superRefine((data, ctx) => {
  const { config } = data;
  const nbSeeds = num(config.nbSeeds);
  if (config.mainDrawSize && (config.mainDrawSize < 4 || config.mainDrawSize > 128)) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: `La taille du tableau doit être entre 4 et 128 (actuellement ${config.mainDrawSize}).`, path: ['config', 'mainDrawSize'] });
  }
  if (config.nbPairsPerPool && config.nbPairsPerPool < 3) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: `Il faut au moins 3 équipes par poule (actuellement ${config.nbPairsPerPool}).`, path: ['config', 'nbPairsPerPool'] });
  }
  if (data.startDate && data.endDate && data.startDate > data.endDate) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'La date de fin doit être postérieure ou égale à la date de début.', path: ['endDate'] });
  }
});

export type TournamentFormData = z.input<typeof TournamentFormSchema>;
export type ParsedTournamentForm = z.infer<typeof TournamentFormSchema>;
