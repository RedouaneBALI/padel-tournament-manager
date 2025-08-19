package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "player1_id")
  private Player player1;
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "player2_id")
  private Player player2;
  private int    seed;

  public PlayerPair(String name1, String name2, int seed) {
    this.player1 = new Player(name1);
    this.player2 = new Player(name2);
    this.seed    = seed;
  }

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

  @JsonProperty("seed")
  public Integer getSeedForJson() {
    return seed == Integer.MAX_VALUE ? null : seed;
  }

  public boolean isBye() {
    if (player1 == null || player2 == null) {
      return false;
    }
    return "BYE".equals(player1.getName()) && "BYE".equals(player2.getName());
  }

  @Override
  public String toString() {
    return player1.getName() + " / " + player2.getName();
  }
}
