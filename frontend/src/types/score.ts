export type GamePoint =
  | 'ZERO'
  | 'QUINZE'
  | 'TRENTE'
  | 'QUARANTE'
  | 'AVANTAGE';

export interface SetScore {
  teamAScore: number | null;
  teamBScore: number | null;
  tieBreakTeamA?: number | null;
  tieBreakTeamB?: number | null;
}

export interface Score {
  sets: SetScore[];
  forfeit?: boolean;
  forfeitedBy?: 'TEAM_A' | 'TEAM_B' | null;
  currentGamePointA?: GamePoint | null;
  currentGamePointB?: GamePoint | null;
  tieBreakPointA?: number | null;
  tieBreakPointB?: number | null;
}