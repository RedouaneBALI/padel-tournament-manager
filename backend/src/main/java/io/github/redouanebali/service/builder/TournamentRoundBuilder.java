package io.github.redouanebali.service.builder;

import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.service.strategy.StrategyResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TournamentRoundBuilder {

  public void validateAndBuild(Tournament tournament) {
    TournamentFormat       format = tournament.getFormat();
    TournamentFormatConfig cfg    = tournament.getConfig();

    FormatStrategy strategy = StrategyResolver.resolve(format);
    List<String>   errors   = new ArrayList<>();
    strategy.validate(cfg, errors);

    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid tournament config: " + errors);
    }

    strategy.buildInitialRounds(tournament, cfg);
  }
}
