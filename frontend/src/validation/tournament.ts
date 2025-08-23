// src/validation/tournament.ts
import { z } from 'zod';

// Enum for tournament format (aligned with backend)
export const TournamentFormatEnum = z.enum(['KNOCKOUT', 'GROUPS_KO', 'QUALIF_KO']);

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

export const TournamentFormatConfigSchema = z.object({
  mainDrawSize: z.preprocess(emptyToNull, intFromInput.nullable()).default(32),
  nbSeeds: z.preprocess(emptyToNull, intFromInput.nullable()).default(null),
  nbPools: z.preprocess(emptyToNull, intFromInput.nullable()).default(4),
  nbPairsPerPool: z.preprocess(emptyToNull, intFromInput.nullable()).default(4),
  nbQualifiedByPool: z.preprocess(emptyToNull, intFromInput.nullable()).default(2),
  preQualDrawSize: z.preprocess(emptyToNull, intFromInput.nullable()).default(16),
  nbQualifiers: z.preprocess(emptyToNull, intFromInput.nullable()).default(2),
});

export const TournamentFormSchema = z.object({
  id: z.number().int().nullable().optional().default(null),
  name: z.string().trim().min(1, 'Le tournoi doit avoir un nom.').default(''),
  description: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  city: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  club: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  gender: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  level: z.preprocess(trimThenEmptyToNull, z.string().nullable()).default(null),
  format: TournamentFormatEnum,
  startDate: z.preprocess(emptyToNull, z.string().nullable()).default(null),
  endDate: z.preprocess(emptyToNull, z.string().nullable()).default(null),
  config: TournamentFormatConfigSchema,
}).superRefine((data, ctx) => {
  const { config, format } = data;
  const nbSeeds = num(config.nbSeeds);
  console.log(data);
  if (format === 'KNOCKOUT') {
    const md = num(config.mainDrawSize);
    if (md < 4 || md > 128) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: `En élimination directe, la taille du tableau doit être entre 4 et 128 (actuellement ${md}).`, path: ['formatConfig', 'mainDrawSize'] });
    }
    const maxSeeds = Math.floor(md / 2);
    if (nbSeeds > maxSeeds) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: `En élimination directe, les têtes de série (${nbSeeds}) ne peuvent pas dépasser la moitié du tableau (${md} → max ${maxSeeds}).`, path: ['formatConfig', 'nbSeeds'] });
    }
  }

  if (format === 'GROUPS_KO') {
    const { nbPools, nbPairsPerPool } = config;
    if (num(nbPairsPerPool) < 3) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: `En poules, il faut au moins 3 équipes par poule (actuellement ${nbPairsPerPool}).`, path: ['formatConfig', 'nbPairsPerPool'] });
    }
    const maxSeeds = num(nbPools) * num(nbPairsPerPool);
    if (nbSeeds > maxSeeds) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: `En poules, les têtes de série (${nbSeeds}) ne peuvent pas dépasser le total d'équipes (${nbPools}×${nbPairsPerPool} = ${maxSeeds}).`, path: ['config', 'nbSeeds'] });
    }
  }
  if (format === 'QUALIF_KO') {
    const { preQualDrawSize, nbQualifiers, nbSeeds } = config;
    if (num(preQualDrawSize) < 2) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `En qualif + élimination directe, la taille du tableau de qualif doit être au moins 2 (actuellement ${preQualDrawSize}).`,
        path: ['config', 'preQualDrawSize'],
      });
    }
    if (num(nbQualifiers) < 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: `Il doit y avoir au moins 1 qualifié en sortie de qualif (actuellement ${nbQualifiers}).`,
        path: ['config', 'nbQualifiers'],
      });
    }
}
  if (data.startDate && data.endDate && data.startDate > data.endDate) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'La date de fin doit être postérieure ou égale à la date de début.', path: ['endDate'] });
  }
});

export type TournamentFormData = z.input<typeof TournamentFormSchema>;
export type ParsedTournamentForm = z.infer<typeof TournamentFormSchema>;