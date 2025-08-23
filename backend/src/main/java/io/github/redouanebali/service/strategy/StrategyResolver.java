package io.github.redouanebali.service.strategy;

import io.github.redouanebali.model.format.FormatStrategy;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.groupsko.GroupsKoStrategy;
import io.github.redouanebali.model.format.knockout.KnockoutStrategy;
import io.github.redouanebali.model.format.qualifymain.QualifMainStrategy;

public class StrategyResolver {

  public static FormatStrategy resolve(TournamentFormat format) {
    return switch (format) {
      case KNOCKOUT -> new KnockoutStrategy();
      case GROUPS_KO -> new GroupsKoStrategy();
      case QUALIF_KO -> new QualifMainStrategy();
    };
  }
}