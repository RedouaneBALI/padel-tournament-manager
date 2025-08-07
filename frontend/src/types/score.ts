export interface SetScore {
  teamAScore: number | null;
  teamBScore: number | null;
}

export interface Score {
  sets: SetScore[];
}