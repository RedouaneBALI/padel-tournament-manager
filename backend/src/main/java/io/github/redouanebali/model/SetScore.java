package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "set_score")
@Getter
@Setter
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