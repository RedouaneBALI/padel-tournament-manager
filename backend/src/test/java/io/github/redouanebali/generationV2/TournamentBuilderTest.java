package io.github.redouanebali.generationV2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TournamentBuilderTest {

  private static Tournament makeTournament(
      int preQualDrawSize,
      int nbQualifiers,
      int mainDrawSize,
      int nbSeeds,
      int nbSeedsQualify,
      DrawMode drawMode) {
    Tournament t = new Tournament();
    TournamentFormatConfig cfg = TournamentFormatConfig.builder()
                                                       .preQualDrawSize(preQualDrawSize)
                                                       .nbQualifiers(nbQualifiers)
                                                       .mainDrawSize(mainDrawSize)
                                                       .nbSeeds(nbSeeds)
                                                       .nbSeedsQualify(nbSeedsQualify)
                                                       .drawMode(drawMode)
                                                       .build();
    t.setConfig(cfg);
    return t;
  }

  private static List<Stage> parseStages(String stagesCsv) {
    return Arrays.stream(stagesCsv.split(";"))
                 .map(String::trim)
                 .map(Stage::valueOf)
                 .collect(Collectors.toList());
  }

  private static List<Integer> parseInts(String csv) {
    return Arrays.stream(csv.split(";"))
                 .map(String::trim)
                 .map(Integer::parseInt)
                 .collect(Collectors.toList());
  }

  @ParameterizedTest(name = "buildQualifKO preQual={0}, nbQualifiers={1}, mainDraw={2}")
  @CsvSource({
      // preQual, nbQualifiers, mainDraw, nbSeedsMain, nbSeedsQual, drawMode,
      // expectedStages, expectedMatchesPerStage
      // A) Direct main draw 32
      "0, 0, 32, 8, 0, SEEDED, R32;R16;QUARTERS;SEMIS;FINAL, 16;8;4;2;1",
      // B) Direct main draw 64
      "0, 0, 64, 16, 0, SEEDED, R64;R32;R16;QUARTERS;SEMIS;FINAL, 32;16;8;4;2;1",
      // C) Qualif 16->4 + main 32 (Q1,Q2 then R32..)
      "16, 4, 32, 8, 4, SEEDED, Q1;Q2;R32;R16;QUARTERS;SEMIS;FINAL, 8;4;16;8;4;2;1",
      // D) Qualif 32->8 + main 32
      "32, 8, 32, 8, 8, SEEDED, Q1;Q2;R32;R16;QUARTERS;SEMIS;FINAL, 16;8;16;8;4;2;1",
      // E) Qualif 32->8 + main 64
      "32, 8, 64, 16, 8, SEEDED, Q1;Q2;R64;R32;R16;QUARTERS;SEMIS;FINAL, 16;8;32;16;8;4;2;1"
  })
  void testBuildQualifKO_createsExpectedRoundsAndMatches(int preQual,
                                                         int nbQualifiers,
                                                         int mainDraw,
                                                         int nbSeedsMain,
                                                         int nbSeedsQual,
                                                         DrawMode drawMode,
                                                         String expectedStagesCsv,
                                                         String expectedMatchesCsv) {
    Tournament t = makeTournament(preQual, nbQualifiers, mainDraw, nbSeedsMain, nbSeedsQual, drawMode);

    TournamentBuilder builder = new TournamentBuilder();
    Tournament        built   = builder.buildQualifKO(t);

    List<Stage>   expectedStages  = parseStages(expectedStagesCsv);
    List<Integer> expectedMatches = parseInts(expectedMatchesCsv);

    List<Stage> actualStages = built.getRounds().stream()
                                    .map(Round::getStage)
                                    .collect(Collectors.toList());

    List<Integer> actualMatches = built.getRounds().stream()
                                       .map(r -> r.getGames() == null ? 0 : r.getGames().size())
                                       .collect(Collectors.toList());

    assertEquals(expectedStages, actualStages, "Stages sequence must match");
    assertEquals(expectedMatches, actualMatches, "Matches per stage must match");

    // Basic sanity: total rounds must be sum of qualification rounds + main draw rounds
    assertEquals(built.getRounds().size(), expectedStages.size(), "Unexpected number of rounds created");
  }
}
