package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Round {

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "round_id") // FK in Game table
  private final List<Game>  games       = new ArrayList<>();
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "round_id")
  private final Set<Pool>   pools       = new LinkedHashSet<>();
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private       Long        id;
  @Enumerated(EnumType.STRING)
  private       Stage       stage;
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "match_format_id")
  private       MatchFormat matchFormat = new MatchFormat();

  public Round(Stage stage) {
    this.stage = stage;
  }

  public void addGame(PlayerPair teamA, PlayerPair teamB) {
    Game game = new Game();
    game.setTeamA(teamA);
    game.setTeamB(teamB);
    game.setFormat(this.matchFormat);
    this.games.add(game);
  }

  public void addGame(Game game) {
    this.games.add(game);
  }

  public void addGames(List<Game> games) {
    this.games.addAll(games);
  }

  public void addPools(List<Pool> pools) {
    this.pools.addAll(pools);
  }

  public void addPool(Pool pool) {
    this.pools.add(pool);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Round round = (Round) o;
    return stage == round.getStage();
  }

  @Override
  public int hashCode() {
    return stage != null ? stage.hashCode() : 0;
  }

}
