package io.github.redouanebali.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class PoolRankingDTO {

  private Long                        id;
  private List<PoolRankingDetailsDTO> details;
}
