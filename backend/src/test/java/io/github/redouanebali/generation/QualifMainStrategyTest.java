package io.github.redouanebali.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.redouanebali.model.format.StageKey;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.model.format.qualifymain.QualifMainStrategy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class QualifMainStrategyTest {

  @Test
  void validate_allowsByes_whenFilledIsLessThanMainDraw_powerOfTwoMaintained() {
    // pre-qual: 32 -> 8 qualifiers (2 tours). main draw is 32 (power of two).
    // Filled entries to main = 24 (12 direct + 4 WC + 8 qualifs) → 8 BYEs expected.
    TournamentFormatConfig c = TournamentFormatConfig.builder()
                                                     .preQualDrawSize(32).nbQualifiers(8).mainDrawSize(32).nbSeeds(8).build();

    QualifMainStrategy s      = new QualifMainStrategy();
    List<String>       errors = new ArrayList<>();
    s.validate(c, errors);

    assertTrue(errors.isEmpty(), "Validation should pass with BYEs (filled < mainDrawSize)");
  }

  @Test
  void validate_rejects_nonPowerOfTwo_preQual() {
    TournamentFormatConfig c = TournamentFormatConfig.builder()
                                                     .preQualDrawSize(30).nbQualifiers(8).mainDrawSize(32).nbSeeds(8).build();

    QualifMainStrategy s      = new QualifMainStrategy();
    List<String>       errors = new ArrayList<>();
    s.validate(c, errors);

    assertTrue(errors.stream().anyMatch(e -> e.contains("preQualDrawSize must be a power of two")));
  }

  @Test
  void validate_rejects_nonPowerOfTwo_mainDraw() {
    TournamentFormatConfig c = TournamentFormatConfig.builder()
                                                     .preQualDrawSize(30).nbQualifiers(8).mainDrawSize(24).nbSeeds(8).build();
    QualifMainStrategy s      = new QualifMainStrategy();
    List<String>       errors = new ArrayList<>();
    s.validate(c, errors);

    assertTrue(errors.stream().anyMatch(e -> e.contains("mainDrawSize must be a power of two")));
  }

  @Test
  void validate_rejects_nbSeeds_greater_than_mainDraw() {
    TournamentFormatConfig c = TournamentFormatConfig.builder()
                                                     .preQualDrawSize(32).nbQualifiers(8).mainDrawSize(24).nbSeeds(33).build();
    QualifMainStrategy s      = new QualifMainStrategy();
    List<String>       errors = new ArrayList<>();
    s.validate(c, errors);

    assertTrue(errors.stream().anyMatch(e -> e.contains("nbSeeds > mainDrawSize")));
  }

  @Test
  void stages_returns_preQualif_then_mainDraw() {
    TournamentFormatConfig c = TournamentFormatConfig.builder()
                                                     .preQualDrawSize(32).nbQualifiers(8).mainDrawSize(32).nbSeeds(8).build();

    QualifMainStrategy s = new QualifMainStrategy();

    List<StageKey> stages = s.stages(c);
    assertEquals(2, stages.size());
    assertEquals(StageKey.PRE_QUALIF, stages.get(0));
    assertEquals(StageKey.MAIN_DRAW, stages.get(1));
  }
}
