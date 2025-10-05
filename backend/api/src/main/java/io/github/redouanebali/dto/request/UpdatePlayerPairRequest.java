package io.github.redouanebali.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePlayerPairRequest {

  private String  player1Name;
  private String  player2Name;
  private Integer seed;

  @AssertTrue(message = "At least one of player1Name, player2Name or seed must be provided")
  public boolean isAnyFieldProvided() {
    boolean hasP1   = player1Name != null && !player1Name.trim().isEmpty();
    boolean hasP2   = player2Name != null && !player2Name.trim().isEmpty();
    boolean hasSeed = seed != null;
    return hasP1 || hasP2 || hasSeed;
  }
}
