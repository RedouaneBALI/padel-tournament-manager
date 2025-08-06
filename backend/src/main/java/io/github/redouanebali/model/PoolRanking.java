package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.util.LinkedList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PoolRanking {

  @Id
  @GeneratedValue
  private Long id;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "pool_ranking_id")
  private List<PoolRankingDetails> details = new LinkedList<>();

  public void addDetails(PoolRankingDetails rankingDetails) {
    details.add(rankingDetails);
  }
}
