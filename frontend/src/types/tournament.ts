//src/types/tournament.ts
import type { Round } from '@/src/types/round';
import type { PlayerPair } from '@/src/types/playerPair';
import type { Stage } from '@/src/types/stage';

// Base fields common to any tournament
export interface BaseTournament {
  id: number;
  name: string;
  description: string;
  city: string;
  club: string;
  gender: string;
  level: string;
  rounds: Round[];
  nbSeeds: number; // kept for quick UI access but also present inside config
  playerPairs: PlayerPair[];
  startDate: string; // ISO format "yyyy-MM-dd"
  endDate: string;   // ISO format "yyyy-MM-dd"
  nbMaxPairs: number; // generic cap applies to any format
  isEditable: boolean;
  currentRoundStage: Stage;
}

// Configs mirror backend structures
export interface KnockoutConfig {
  mainDrawSize: number; // power of two
  nbSeeds: number;
}

export interface GroupsKoConfig {
  nbPools: number;
  nbPairsPerPool: number;
  nbQualifiedByPool: number;
  mainDrawSize: number; // e.g., nbPools * nbQualifiedByPool
  nbSeeds: number;
}

export type TournamentFormat = 'KNOCKOUT' | 'GROUPS_KO';

export type Tournament =
  | (BaseTournament & {
      tournamentFormat: 'KNOCKOUT';
      formatConfig: KnockoutConfig;
    })
  | (BaseTournament & {
      tournamentFormat: 'GROUPS_KO';
      formatConfig: GroupsKoConfig;
    });

// Type guards for convenience
export function isKnockout(t: Tournament): t is BaseTournament & { tournamentFormat: 'KNOCKOUT'; formatConfig: KnockoutConfig } {
  return t.tournamentFormat === 'KNOCKOUT';
}

export function isGroupsKo(t: Tournament): t is BaseTournament & { tournamentFormat: 'GROUPS_KO'; formatConfig: GroupsKoConfig } {
  return t.tournamentFormat === 'GROUPS_KO';
}

// Helpers to safely access config-specific values without casts
export function getKnockoutConfig(t?: Tournament): KnockoutConfig | null {
  return t && isKnockout(t) ? t.formatConfig : null;
}

export function getGroupsConfig(t?: Tournament): GroupsKoConfig | null {
  return t && isGroupsKo(t) ? t.formatConfig : null;
}

export function getNbQualifiedByPool(t?: Tournament, fallback = 1): number {
  const cfg = getGroupsConfig(t);
  return cfg ? cfg.nbQualifiedByPool : fallback;
}
