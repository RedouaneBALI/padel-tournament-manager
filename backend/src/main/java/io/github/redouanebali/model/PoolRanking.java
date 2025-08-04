package io.github.redouanebali.model;

import jakarta.persistence.ElementCollection;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PoolRanking {

  @ElementCollection
  private List<PoolRankingDetails> details = new LinkedList<>();

  public void addDetails(PoolRankingDetails rankingDetails) {
    details.add(rankingDetails);
  }
}
