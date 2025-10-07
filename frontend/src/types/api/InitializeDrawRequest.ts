// src/types/api/InitializeDrawRequest.ts

// Types sent from the front to initialize a manual draw on the backend.
// Minimal, explicit, and format-agnostic (we start with KNOCKOUT first-round use case).

import { PairType } from '@/src/types/pairType';

export interface TeamSlotRequest {
  /** Slot type: a concrete pair by id, a BYE placeholder, or a QUALIFIER placeholder */
  type: PairType;
  /** Required when type === 'NORMAL' */
  pairId?: number;
}

export interface GameRequest {
  /** teamA can be a TeamSlot or null if intentionally left empty */
  teamA: TeamSlotRequest | null;
  /** teamB can be a TeamSlot or null if intentionally left empty */
  teamB: TeamSlotRequest | null;
}

export interface MatchFormatRequest {
  // Keep minimal & optional; backend may ignore/override
  bestOfSets?: number;
  gamesPerSet?: number;
  tieBreak?: boolean;
  // Allow future extension without breaking callers
  [key: string]: unknown;
}

export interface RoundRequest {
  /** e.g. 'R16', 'QUARTERS', 'SEMI', 'FINAL' */
  stage: string;
  games: GameRequest[];
  matchFormat?: MatchFormatRequest;
}

export interface InitializeDrawRequest {
  /** For KO manual init, send exactly one round (the first/main draw round) */
  rounds: RoundRequest[];
}