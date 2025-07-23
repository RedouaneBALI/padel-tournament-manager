package io.github.redouanebali.dto;

import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SimplePlayerPairDTO {
  private String player1;
  private String player2;
  private Integer seed;

  public PlayerPair toPlayerPair() {
    Player p1 = new Player();
    p1.setName(player1);

    Player p2 = new Player();
    p2.setName(player2);

    return new PlayerPair(null, p1, p2, seed);
  }

  public static SimplePlayerPairDTO fromPlayerPair(PlayerPair pair) {
    if (pair == null) return null;
    String p1Name = pair.getPlayer1() != null ? pair.getPlayer1().getName() : "";
    String p2Name = pair.getPlayer2() != null ? pair.getPlayer2().getName() : "";
    return new SimplePlayerPairDTO(p1Name, p2Name, pair.getSeed());
  }
}
