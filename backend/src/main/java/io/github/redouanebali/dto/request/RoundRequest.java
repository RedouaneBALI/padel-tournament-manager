package io.github.redouanebali.dto.request;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoundRequest {

  private String            stage;                  // ex: "R16"
  private List<GameRequest> games = new ArrayList<>();
}
