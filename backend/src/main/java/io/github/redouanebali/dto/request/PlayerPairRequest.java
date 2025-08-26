package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.PairType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerPairRequest {

  private PairType type;
  private Long     pairId; // requis si type == "PAIR"

  public boolean isBye() {
    return this.type == PairType.BYE;
  }

  public boolean isQualifier() {
    return this.type == PairType.QUALIFIER;
  }
}