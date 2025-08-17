import type { Round } from '@/src/types/round';
import type { PlayerPair } from '@/src/types/playerPair';
import type { Stage } from '@/src/types/stage';

export interface Tournament {
  id: number;
  name: string;
  description: string;
  city: string;
  club: string;
  gender: string;
  level: string;
  tournamentFormat: 'KNOCKOUT' | 'GROUP_STAGE';
  rounds: Round[];
  nbSeeds: number;
  playerPairs: PlayerPair[];
  startDate: string; // ISO format "yyyy-MM-dd"
  endDate: string;   // ISO format "yyyy-MM-dd"
  nbMaxPairs: number;
  nbPools: number;
  nbPairsPerPool: number;
  nbQualifiedByPool: number;
  isEditable: boolean;
  currentRoundStage: Stage;
}
