package io.github.redouanebali.api.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class ScoreDTO {

  private List<SetScoreDTO> sets;
}
