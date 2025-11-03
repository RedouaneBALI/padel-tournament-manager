package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @JsonIgnore
  private int      seed;
  @Enumerated(EnumType.STRING)
  private PairType type = PairType.NORMAL;

  /**
   * Index of the qualifier (1 for Q1, 2 for Q2, etc.). Only used when type = QUALIFIER, null for all other types. This allows us to persist and
   * display the correct qualifier number (Q1, Q2, Q3...).
   */
  private Integer qualifierIndex;

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

  /**
   * Creates a QUALIFIER placeholder with a specific number (Q1, Q2, Q3, etc.) The qualifier index is stored in the qualifierIndex field for clean
   * persistence.
   *
   * @param index the qualifier number (1-based)
   * @return a PlayerPair representing a qualifier slot
   */
  public static PlayerPair qualifier(int index) {
    Player     q1    = new Player("Q" + index);
    Player     q2    = new Player("Q" + index);
    PlayerPair qPair = new PlayerPair();
    qPair.setPlayer1(q1);
    qPair.setPlayer2(q2);
    qPair.setSeed(Integer.MAX_VALUE);
    qPair.setType(PairType.QUALIFIER);
    qPair.setQualifierIndex(index); // Store the qualifier number in dedicated field
    return qPair;
  }


  public boolean isBye() {
    return type == PairType.BYE;
  }

  public boolean isQualifier() {
    return type == PairType.QUALIFIER;
  }

  @Override
  public String toString() {
    if (type == PairType.BYE) {
      return "BYE";
    }
    if (type == PairType.QUALIFIER) {
      return player1 != null ? player1.getName() : "Q";
    }
    return (player1 != null && player2 != null)
           ? player1.getName() + " / " + player2.getName()
           : "";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlayerPair that = (PlayerPair) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}