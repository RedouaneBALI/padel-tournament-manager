package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Group {

  @ManyToMany
  private final Set<PlayerPair>           pairs   = new LinkedHashSet<>();
  @Id
  @GeneratedValue
  private       Long                      id;
  private       String                    name;
  private       List<GroupRankingDetails> ranking = new LinkedList<>();

  public Group(String name, List<PlayerPair> pairs) {
    this.name = name;
    if (pairs != null) {
      this.pairs.addAll(pairs);
      pairs.forEach(pair -> ranking.add(new GroupRankingDetails(pair, 0, 0)));
    }
  }

  public void addPair(PlayerPair pair) {
    this.pairs.add(pair);
    ranking.add(new GroupRankingDetails(pair, 0, 0));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Group " + name + " :\n");
    for (GroupRankingDetails details : ranking) {
      sb.append("  ").append(details).append("\n");
    }
    return sb.toString();
  }

}