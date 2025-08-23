package io.github.redouanebali.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PoolDTO {

  private Long                id;
  private String              name;
  private List<PlayerPairDTO> pairs = new ArrayList<>();
  private PoolRankingDTO      poolRanking;
}
