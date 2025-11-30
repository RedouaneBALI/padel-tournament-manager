package io.github.redouanebali.model;

import jakarta.persistence.Column;
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

  @Column(name = "team_a_score")
  private Integer teamAScore;

  @Column(name = "team_b_score")
  private Integer teamBScore;

  @Column(name = "tie_break_team_a")
  private Integer tieBreakTeamA; // null si pas de tie-break

  @Column(name = "tie_break_team_b")
  private Integer tieBreakTeamB;

  public SetScore(Integer teamAScore, Integer teamBScore) {
    this.teamAScore = teamAScore;
    this.teamBScore = teamBScore;
  }

  public SetScore(Integer teamAScore, Integer teamBScore, Integer tieBreakTeamA, Integer tieBreakTeamB) {
    this.teamAScore    = teamAScore;
    this.teamBScore    = teamBScore;
    this.tieBreakTeamA = tieBreakTeamA;
    this.tieBreakTeamB = tieBreakTeamB;
  }
}