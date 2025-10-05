package io.github.redouanebali.api.dto.response;

import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TournamentLevel;
import io.github.redouanebali.model.format.TournamentConfig;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentDTO {

  private Long                id;
  private String              ownerId;
  private String              name;
  private List<RoundDTO>      rounds;
  private List<PlayerPairDTO> playerPairs;
  private String              description;
  private String              city;
  private String              club;
  private Gender              gender;
  private TournamentLevel     level;
  private TournamentConfig    config;
  private LocalDate           startDate;
  private LocalDate           endDate;

  public Stage getCurrentRoundStage() {
    if (rounds == null || rounds.isEmpty()) {
      return null;
    }

    Stage lastUsedStage = null; // last round where at least one team is assigned

    // Iterate from earliest to latest
    for (RoundDTO r : rounds) {
      if (r.getGames() == null || r.getGames().isEmpty()) {
        continue;
      }

      // A round is considered "used" if at least one game has a team assigned (A or B)
      boolean hasAssigned = r.getGames().stream()
                             .anyMatch(g -> g.getTeamA() != null || g.getTeamB() != null);
      if (!hasAssigned) {
        continue; // placeholder round → ignore
      }

      lastUsedStage = r.getStage();

      // Current round = first (earliest) used round where there exists a game to play
      boolean existsUnfinished = r.getGames().stream()
                                  .anyMatch(g -> (g.getTeamA() != null || g.getTeamB() != null) && !g.isFinished());
      if (existsUnfinished) {
        return r.getStage();
      }
    }

    // No unfinished games left → return the last used stage if any, else the first stage
    if (lastUsedStage != null) {
      return lastUsedStage;
    }

    return rounds.get(0).getStage();
  }

}
