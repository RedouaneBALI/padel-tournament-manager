package io.github.redouanebali.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Round {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long        id;
  private Stage       stage;
  @OneToMany
  private List<Game>  games       = new ArrayList<>();
  @Embedded
  private MatchFormat matchFormat = new MatchFormat();

  public Round(Stage stage) {
    this.stage = stage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Round round = (Round) o;
    return stage == round.getStage();
  }

  @Override
  public int hashCode() {
    return stage != null ? stage.hashCode() : 0;
  }

}
