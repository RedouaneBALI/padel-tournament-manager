import type { Round } from '@/src/types/round';
import type { PlayerPair } from '@/src/types/playerPair';
import type { Stage } from '@/src/types/stage';

export interface TournamentFormatConfig {
  mainDrawSize: number;
  nbSeeds: number;
  nbPools?: number;
  nbPairsPerPool?: number;
  nbQualifiedByPool?: number;
  preQualDrawSize?: number;
  numQualifiers?: number;
  nbSeedsQualify?: number;
}

export type TournamentFormat = 'KNOCKOUT' | 'GROUPS_KO' | 'QUALIF_KO';

export interface Tournament {
  id: number;
  name: string;
  description: string;
  city: string;
  club: string;
  gender: string;
  level: string;
  rounds: Round[];
  playerPairs: PlayerPair[];
  startDate: string;
  endDate: string;
  isEditable: boolean;
  currentRoundStage: Stage;
  format: TournamentFormat;
  config: TournamentFormatConfig;
}
