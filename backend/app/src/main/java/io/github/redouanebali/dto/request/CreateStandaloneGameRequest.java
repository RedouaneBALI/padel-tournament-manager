package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.MatchFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a standalone game (match) without a tournament. Contains two player pairs and a match format.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateStandaloneGameRequest {

  @NotNull(message = "Team A is required")
  private CreatePlayerPairRequest teamA;

  @NotNull(message = "Team B is required")
  private CreatePlayerPairRequest teamB;

  @NotNull(message = "Match format is required")
  private MatchFormat format;
}

