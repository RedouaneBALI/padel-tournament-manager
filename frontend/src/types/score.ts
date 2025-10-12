export interface SetScore {
  teamAScore: number | null;
  teamBScore: number | null;
}

export interface Score {
  sets: SetScore[];
  forfeit?: boolean;
  forfeitedBy?: 'TEAM_A' | 'TEAM_B' | null;
}