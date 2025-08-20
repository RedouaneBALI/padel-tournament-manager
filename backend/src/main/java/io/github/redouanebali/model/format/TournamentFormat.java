package io.github.redouanebali.model.format;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TournamentFormat {
  KNOCKOUT(new KnockoutStrategy(), KnockoutConfig.class),
  GROUPS_KO(new GroupsKoStrategy(), GroupsKoConfig.class),
  QUALIF_MAIN(new QualifMainStrategy(), QualifMainConfig.class);

  @Getter
  private final FormatStrategy<?>                 strategy;
  @Getter
  private final Class<? extends TournamentConfig> configClass;
}