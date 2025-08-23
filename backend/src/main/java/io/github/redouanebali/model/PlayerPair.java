package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlayerPair {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "player1_id")
  private Player player1;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "player2_id")
  private Player player2;

  private int      seed;
  @Enumerated(EnumType.STRING)
  private PairType type = PairType.NORMAL;

  public PlayerPair(Player player1, Player player2, int seed) {
    this.player1 = player1;
    this.player2 = player2;
    this.seed    = seed;
  }

  public PlayerPair(String player1Name, String player2Name, int seed) {
    this.player1 = new Player(player1Name);
    this.player2 = new Player(player2Name);
    this.seed    = seed;
  }

  // --- factories ---
  public static PlayerPair bye() {
    Player     bye1    = new Player("BYE");
    Player     bye2    = new Player("BYE");
    PlayerPair byePair = new PlayerPair();
    byePair.setPlayer1(bye1);
    byePair.setPlayer2(bye2);
    byePair.setSeed(Integer.MAX_VALUE);
    byePair.setType(PairType.BYE);
    return byePair;
  }

  public static PlayerPair qualifierSlot(String label, int slotNumber) {
    Player     placeholder1 = new Player(label + " #" + slotNumber);
    Player     placeholder2 = new Player(label + " #" + slotNumber);
    PlayerPair qPair        = new PlayerPair();
    qPair.setPlayer1(placeholder1);
    qPair.setPlayer2(placeholder2);
    qPair.setSeed(Integer.MAX_VALUE - 1); // ou un code spécial
    qPair.setType(PairType.QUALIFIER);
    return qPair;
  }

  public boolean isBye() {
    return type == PairType.BYE;
  }

  public boolean isQualifierSlot() {
    return type == PairType.QUALIFIER;
  }

  @Override
  public String toString() {
    if (type == PairType.BYE) {
      return "BYE";
    }
    if (type == PairType.QUALIFIER) {
      return "Q";
    }
    return (player1 != null && player2 != null)
           ? player1.getName() + " / " + player2.getName()
           : "";
  }
}