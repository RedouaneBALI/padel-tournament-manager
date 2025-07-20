package io.github.redouanebali.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
  private Long             id;
  private RoundInfo        info;
  @ElementCollection
  private List<PlayerPair> playerPairs = new ArrayList<>();
  @ElementCollection
  private List<Game>       games       = new ArrayList<>();
  private MatchFormat      matchFormat;

  public Round(RoundInfo roundInfo) {
    this.info = roundInfo;
  }

}
