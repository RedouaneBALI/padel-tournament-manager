package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Tournament;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerPairRequest {

  private PairType type;
  private Long     pairId; // requis si type == "PAIR"

  public static PlayerPair toModel(PlayerPairRequest req, Tournament tournament) {
    if (req == null) {
      return null;
    }
    if (req.isBye()) {
      return PlayerPair.bye();
    }
    if (req.isQualifier()) {
      return PlayerPair.qualifier();
    }
    if (req.getType() == PairType.NORMAL && req.getPairId() != null) {
      return tournament.getPlayerPairs().stream()
                       .filter(p -> req.getPairId().equals(p.getId()))
                       .findFirst()
                       .orElse(null);
    }
    return null;
  }

  public static PlayerPairRequest fromModel(PlayerPair pair) {
    if (pair == null) {
      return null;
    }
    PlayerPairRequest req = new PlayerPairRequest();
    req.setType(pair.getType());
    req.setPairId(pair.getId());
    return req;
  }

  public boolean isBye() {
    return this.type == PairType.BYE;
  }

  public boolean isQualifier() {
    return this.type == PairType.QUALIFIER;
  }
}