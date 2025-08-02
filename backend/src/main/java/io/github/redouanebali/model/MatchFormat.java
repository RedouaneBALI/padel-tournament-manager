package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchFormat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Min(1)
  private int     numberOfSetsToWin       = 2;        // Ex : 2 pour un match en 2 sets gagnants
  @Min(1)
  private int     pointsPerSet            = 6;             // Ex : 6 jeux pour gagner un set
  private boolean superTieBreakInFinalSet = false; // true si le 3e set est un super tie-break
  private boolean advantage               = true;             // true = jeu à l'avantage, false = no-ad

}
