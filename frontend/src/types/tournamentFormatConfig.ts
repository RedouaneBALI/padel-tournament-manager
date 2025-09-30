export type DrawMode = 'SEEDED' | 'MANUAL';
export type TournamentFormat = 'KNOCKOUT' | 'GROUPS_KO' | 'QUALIF_KO';

export interface TournamentFormatConfig {
  format: TournamentFormat;
  mainDrawSize: number;
  nbSeeds: number | null;
  drawMode?: DrawMode;
  nbPools?: number | null;
  nbPairsPerPool?: number | null;
  nbQualifiedByPool?: number | null;
  preQualDrawSize?: number | null;
  nbQualifiers?: number | null;
  nbSeedsQualify?: number | null;
}
