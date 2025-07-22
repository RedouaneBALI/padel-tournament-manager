package io.github.redouanebali.model;

import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Embeddable
@Data
public class Score {

  private List<SetScore> sets = new ArrayList<>();


  public void addSetScore(int teamAScore, int teamBScore) {
    sets.add(new SetScore(teamAScore, teamBScore));
  }

  public void addSetScore(int teamAScore, int teamBScore, int tieBreakAScore, int tieBreakBScore) {
    sets.add(new SetScore(teamAScore, teamBScore, tieBreakAScore, tieBreakBScore));
  }

}