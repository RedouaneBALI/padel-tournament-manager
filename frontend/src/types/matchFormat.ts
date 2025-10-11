export interface MatchFormat {
  numberOfSetsToWin: number;
  gamesPerSet: number;
  tieBreakAt: number;
  superTieBreakInFinalSet: boolean;
  advantage: boolean;
}