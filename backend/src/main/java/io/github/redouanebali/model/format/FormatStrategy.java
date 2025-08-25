package io.github.redouanebali.model.format;

import io.github.redouanebali.dto.request.InitializeDrawRequest;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import java.util.List;

public interface FormatStrategy {

  void validate(TournamentFormatConfig cfg, List<String> errors);

  /**
   * Retourne le plan des phases (ex: [MAIN_DRAW], [GROUPS, MAIN_DRAW], [PRE_QUALIF, MAIN_DRAW])
   */
  List<StageKey> stages(TournamentFormatConfig cfg);

  /**
   * Génère un round en fonction du mode (manuel vs algorithmique). Par défaut non supporté par la stratégie (à surcharger si applicable).
   */
  List<Round> initializeRounds(Tournament t, List<PlayerPair> pairs, boolean manual);

  void applyManualInitialization(Tournament t, InitializeDrawRequest req);

  /**
   * Propage les vainqueurs du tournoi selon la logique du format (optionnel). Implémentation par défaut: no-op.
   */
  default void propagateWinners(Tournament t) {

  }
}