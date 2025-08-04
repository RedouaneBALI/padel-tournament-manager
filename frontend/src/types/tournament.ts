import { PlayerPair } from '@/src/types/playerPair';
import { Round } from '@/src/types/round';

// types/tournament.ts
export interface Tournament {
  id?: number;
  name: string;
  description?: string;
  city?: string;
  club?: string;
  gender?: 'MALE' | 'FEMALE' | 'MIXED';
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'; // adapte à tes enums Java
  tournamentFormat?: 'SINGLE_ELIMINATION' | 'ROUND_ROBIN' | 'DOUBLE_ELIMINATION'; // idem
  nbSeeds: number;
  playerPairs?: PlayerPair[];
  rounds?: Round[];
  startDate?: string;  // format ISO recommandé : "2025-07-24"
  endDate?: string;
  nbMaxPairs: number;
  nbGroups: number;
  nbPairsPerGroup: number;
  nbQualifiedByGroup: number;
}
