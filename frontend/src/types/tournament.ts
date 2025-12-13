import type { Round } from '@/src/types/round';
import type { PlayerPair } from '@/src/types/playerPair';
import type { Stage } from '@/src/types/stage';
import { TournamentFormatConfig } from '@/src/types/tournamentFormatConfig';

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
  config: TournamentFormatConfig;
  ownerId: string;
  editorIds: string[];
  organizerName?: string;
  tvUrl?: string | null;
}
