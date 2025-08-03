package io.github.redouanebali.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private Stage stage;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "round_id") // FK in Game table
  private List<Game> games = new ArrayList<>();

  @ManyToOne(cascade = CascadeType.ALL)
  private MatchFormat matchFormat = new MatchFormat();

  @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Group> groups = new LinkedHashSet<>();

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
