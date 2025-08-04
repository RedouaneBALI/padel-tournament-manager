package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
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

  @ManyToMany
  private final Set<PlayerPair> pairs       = new LinkedHashSet<>();
  @Id
  @GeneratedValue
  private       Long            id;
  private       String          name;
  @Transient
  private       PoolRanking     poolRanking = new PoolRanking();

  public Pool(String name, List<PlayerPair> pairs) {
    this.name = name;
    if (pairs != null) {
      this.pairs.addAll(pairs);
      pairs.forEach(pair -> poolRanking.addDetails(new PoolRankingDetails(pair, 0, 0)));
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

}