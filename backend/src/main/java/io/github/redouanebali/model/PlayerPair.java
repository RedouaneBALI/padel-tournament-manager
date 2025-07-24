package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PlayerPair {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long   id;
  @ManyToOne
  private Player player1;
  @ManyToOne
  private Player player2;
  private int    seed;

  @Override
  public String toString() {
    return player1 + " - " + player2 + " : " + seed;
  }
}

