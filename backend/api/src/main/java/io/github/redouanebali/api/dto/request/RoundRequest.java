package io.github.redouanebali.api.dto.request;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoundRequest {

  private String            stage;                  // ex: "R16"
  private List<GameRequest> games = new ArrayList<>();

  public static Round toModel(RoundRequest req, Tournament tournament) {
    Stage      stage = Stage.valueOf(req.getStage());
    Round      round = new Round(stage);
    List<Game> games = new ArrayList<>();
    for (GameRequest gReq : req.getGames()) {
      Game game = new Game(); // @todo add format ?
      game.setTeamA(PlayerPairRequest.toModel(gReq.getTeamA(), tournament));
      game.setTeamB(PlayerPairRequest.toModel(gReq.getTeamB(), tournament));
      games.add(game);
    }
    round.replaceGames(games);
    return round;
  }
}
