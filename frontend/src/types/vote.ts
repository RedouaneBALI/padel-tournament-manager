/**
 * Represents the summary of votes for a game.
 */
export interface VoteSummary {
  teamAVotes: number;
  teamBVotes: number;
  currentUserVote: 'TEAM_A' | 'TEAM_B' | null;
}

/**
 * Payload for submitting a vote.
 */
export interface VotePayload {
  teamSide: 'TEAM_A' | 'TEAM_B';
}
