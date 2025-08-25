package io.github.redouanebali.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerPairRequest {

  private String type;   // "PAIR" | "BYE" | "QUALIFIER"
  private Long   pairId; // requis si type == "PAIR"
}