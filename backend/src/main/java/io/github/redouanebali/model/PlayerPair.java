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

  public static PlayerPair bye() {
    Player bye1 = new Player();
    bye1.setName("BYE");

    Player bye2 = new Player();
    bye2.setName("BYE");

    PlayerPair byePair = new PlayerPair();
    byePair.setPlayer1(bye1);
    byePair.setPlayer2(bye2);
    byePair.setSeed(Integer.MAX_VALUE);
    return byePair;
  }

  public boolean isBye() {
    return "BYE".equals(player1.getName()) && "BYE".equals(player2.getName());
  }

  @Override
  public String toString() {
    return player1 + " - " + player2 + " : " + seed;
  }
}

