package io.github.redouanebali.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Score {

  private List<SetScore> sets = new ArrayList<>();
  private Integer        superTieBreakTeamA;
  private Integer        superTieBreakTeamB;

  public void addSetScore(int teamAScore, int teamBScore) {
    sets.add(new SetScore(teamAScore, teamBScore, null, null));
  }

  public void addSetScore(int teamAScore, int teamBScore, Integer tieBreakTeamA, Integer tieBreakTeamB) {
    sets.add(new SetScore(teamAScore, teamBScore, tieBreakTeamA, tieBreakTeamB));
  }
}