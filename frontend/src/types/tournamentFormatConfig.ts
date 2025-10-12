export type DrawMode = 'SEEDED' | 'MANUAL';
export type TournamentFormat = 'KNOCKOUT' | 'GROUPS_KO' | 'QUALIF_KO';

export interface TournamentFormatConfig {
  format: TournamentFormat;
  mainDrawSize: number;
  nbSeeds: number;
  drawMode?: DrawMode;
  staggeredEntry?: boolean;
  nbPools?: number;
  nbPairsPerPool?: number;
  nbQualifiedByPool?: number;
  preQualDrawSize?: number;
  nbQualifiers?: number;
  nbSeedsQualify?: number;
}

