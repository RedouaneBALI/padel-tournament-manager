package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetScore {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int     teamAScore;
  private int     teamBScore;
  private Integer tieBreakTeamA; // Null si pas de tie-break
  private Integer tieBreakTeamB;

  public SetScore(int teamAScore, int teamBScore) {
    this.teamAScore = teamAScore;
    this.teamBScore = teamBScore;
  }

  public SetScore(int teamAScore, int teamBScore, int tieBreakTeamA, int tieBreakTeamB) {
    this.teamAScore    = teamAScore;
    this.teamBScore    = teamBScore;
    this.tieBreakTeamA = tieBreakTeamA;
    this.tieBreakTeamB = tieBreakTeamB;
  }
}