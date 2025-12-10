package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.TeamSide;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {

  @NotNull(message = "Team side is required")
  private TeamSide teamSide;

}

