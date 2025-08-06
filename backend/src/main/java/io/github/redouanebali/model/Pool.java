package io.github.redouanebali.model;

import io.github.redouanebali.service.GroupRankingService;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pool {

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "pool_id")
  private final Set<PlayerPair> pairs       = new LinkedHashSet<>();
  @Id
  @GeneratedValue
  private       Long            id;
  private       String          name;
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "pool_ranking_id")
  private       PoolRanking     poolRanking = new PoolRanking();

  public Pool(String name, List<PlayerPair> pairs) {
    this.name = name;
    if (pairs != null) {
      this.pairs.addAll(pairs);
      initRanking();
    }
  }

  public void addPair(PlayerPair pair) {
    this.pairs.add(pair);
    poolRanking.addDetails(new PoolRankingDetails(pair, 0, 0));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Group " + name + " :\n");
    for (PoolRankingDetails details : poolRanking.getDetails()) {
      sb.append("  ").append(details).append("\n");
    }
    return sb.toString();
  }

  public void initRanking() {
    pairs.forEach(pair -> poolRanking.addDetails(new PoolRankingDetails(pair, 0, 0)));
  }

  public void initPairs(final Set<PlayerPair> pairs) {
    this.pairs.addAll(pairs);
    for (PlayerPair pair : pairs) {
      poolRanking.addDetails(new PoolRankingDetails(pair, 0, 0));
    }
  }

  public void recalculateRanking(final List<Game> poolGames) {
    List<PoolRankingDetails> newRanking = GroupRankingService.computeRanking(this, poolGames);
    PoolRanking              ranking    = new PoolRanking();
    ranking.setDetails(newRanking);
    this.setPoolRanking(ranking);
  }
}