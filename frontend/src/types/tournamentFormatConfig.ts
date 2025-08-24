export type DrawMode = 'SEEDED' | 'MANUAL';

export interface TournamentFormatConfig {
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
