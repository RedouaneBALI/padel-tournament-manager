package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "score_id")
  private List<SetScore> sets = new ArrayList<>();

  public static Score fromString(String scoreStr) {
    Score score = new Score();
    if (scoreStr == null || scoreStr.isBlank()) {
      return score;
    }
    String[] setStrings = scoreStr.trim().split("[,\\s]+");
    for (String setStr : setStrings) {
      String[] parts = setStr.split("-");
      if (parts.length == 2) {
        try {
          int teamAScore = Integer.parseInt(parts[0]);
          int teamBScore = Integer.parseInt(parts[1]);
          score.addSetScore(teamAScore, teamBScore);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid score format: " + setStr, e);
        }
      } else {
        throw new IllegalArgumentException("Invalid set score format: " + setStr);
      }
    }
    return score;
  }

  public void addSetScore(int teamAScore, int teamBScore) {
    sets.add(new SetScore(teamAScore, teamBScore));
  }

  public void addSetScore(int teamAScore, int teamBScore, int tieBreakAScore, int tieBreakBScore) {
    sets.add(new SetScore(teamAScore, teamBScore, tieBreakAScore, tieBreakBScore));
  }

  @Override
  public String toString() {
    return sets.stream()
               .map(set -> set.getTeamAScore() + "-" + set.getTeamBScore())
               .collect(Collectors.joining(" "));
  }

}