package io.github.redouanebali.dto;

import io.github.redouanebali.model.Round;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoundDTO {

  private String         roundName;
  private List<GameInfo> games;

  public RoundDTO(Round round) {
    this.roundName = round.getInfo().name();
    this.games     = round.getGames().stream().map(game ->
                                                       new GameInfo(
                                                           game.getId(),
                                                           new TeamInfo(
                                                               game.getTeamA().getPlayer1().getName(),
                                                               game.getTeamA().getPlayer2().getName(),
                                                               game.getTeamA().getSeed()
                                                           ),
                                                           new TeamInfo(
                                                               game.getTeamB().getPlayer1().getName(),
                                                               game.getTeamB().getPlayer2().getName(),
                                                               game.getTeamB().getSeed()
                                                           )
                                                       )
    ).toList();
  }

  @Data
  @AllArgsConstructor
  public static class GameInfo {

    private Long     id;
    private TeamInfo teamA;
    private TeamInfo teamB;

  }

  @Data
  @AllArgsConstructor
  public static class TeamInfo {

    private String player1;
    private String player2;
    private int    seedNb;

  }
}
