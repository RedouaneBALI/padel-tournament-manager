package io.github.redouanebali.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchFormat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Min(1)
  @Column(name = "number_of_sets_to_win")
  private int     numberOfSetsToWin       = 2;        // Ex : 2 pour un match en 2 sets gagnants
  @Min(1)
  @Column(name = "points_per_set")
  private int     pointsPerSet            = 6;             // Ex : 6 jeux pour gagner un set
  @Column(name = "super_tie_break_in_final_set")
  private boolean superTieBreakInFinalSet = false; // true si le 3e set est un super tie-break
  private boolean advantage               = true;             // true = jeu à l'avantage, false = no-ad

}
