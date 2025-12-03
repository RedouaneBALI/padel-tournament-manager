package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Pool {

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "pool_id")
  private final List<PlayerPair> pairs       = new ArrayList<>();
  @Id
  @GeneratedValue
  private       Long             id;
  private       String           name;
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "pool_ranking_id")
  private       PoolRanking      poolRanking = new PoolRanking();

  public Pool(String name, List<PlayerPair> pairs) {
    this.name = name;
    if (pairs != null) {
      this.pairs.addAll(pairs);
      initRanking();
    }
  }

  public static List<PoolRanking> getGroupRankings(Tournament tournament) {
    return tournament.getRounds().stream()
                     .filter(round -> round.getStage() == Stage.GROUPS)
                     .flatMap(round -> round.getPools().stream())
                     .map(Pool::getPoolRanking)
                     .toList();
  }

  public List<PoolRankingDetails> computeRanking(List<Game> allGamesOfRound) {
    class SetStats {

      int points    = 0;
      int gamesWon  = 0;
      int gamesLost = 0;
    }

    Map<PlayerPair, SetStats> statsMap = new HashMap<>();

    for (PlayerPair pair : this.pairs) {
      statsMap.put(pair, new SetStats());
    }

    for (Game game : allGamesOfRound) {
      PlayerPair teamA = game.getTeamA();
      PlayerPair teamB = game.getTeamB();

      if (this.pairs.contains(teamA) && this.pairs.contains(teamB) && game.isFinished()) {
        TeamSide winner = game.getWinnerSide();
        if (winner == TeamSide.TEAM_A) {
          statsMap.get(teamA).points += 1;
        } else if (winner == TeamSide.TEAM_B) {
          statsMap.get(teamB).points += 1;
        }

        int numSets = game.getScore().getSets().size();
        for (int i = 0; i < numSets; i++) {
          int teamAScore = game.getScore().getSets().get(i).getTeamAScore();
          int teamBScore = game.getScore().getSets().get(i).getTeamBScore();

          statsMap.get(teamA).gamesWon += teamAScore;
          statsMap.get(teamA).gamesLost += teamBScore;
          statsMap.get(teamB).gamesWon += teamBScore;
          statsMap.get(teamB).gamesLost += teamAScore;
        }
      }
    }

    List<PoolRankingDetails> result = statsMap.entrySet().stream()
                                              .sorted((e1, e2) -> {
                                                int cmp = Integer.compare(e2.getValue().points, e1.getValue().points);
                                                if (cmp != 0) {
                                                  return cmp;
                                                }
                                                int diff1 = e1.getValue().gamesWon - e1.getValue().gamesLost;
                                                int diff2 = e2.getValue().gamesWon - e2.getValue().gamesLost;
                                                return Integer.compare(diff2, diff1);
                                              })
                                              .map(entry -> new PoolRankingDetails(
                                                  entry.getKey(),
                                                  entry.getValue().points,
                                                  entry.getValue().gamesWon - entry.getValue().gamesLost
                                              ))
                                              .toList();
    PoolRanking poolRanking = new PoolRanking();
    poolRanking.setDetails(result);
    this.setPoolRanking(poolRanking);
    return result;
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
}