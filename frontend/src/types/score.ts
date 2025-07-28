export interface SetScore {
  teamAScore: number;
  teamBScore: number;
}

export interface Score {
  sets: SetScore[];
}