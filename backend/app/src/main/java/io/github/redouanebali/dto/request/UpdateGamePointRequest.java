package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.TeamSide;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateGamePointRequest {

  @NotNull
  private TeamSide teamSide;
  @NotNull
  private Boolean  increment; // true = +, false = -
}

