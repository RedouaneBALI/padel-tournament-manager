package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class StageTest {

  @ParameterizedTest(name = "fromNbTeams({0}) should return {1}")
  @CsvSource({
      "2, FINAL",
      "3, SEMIS",
      "4, SEMIS",
      "5, QUARTERS",
      "8, QUARTERS",
      "9, R16",
      "16, R16",
      "17, R32",
      "32, R32",
      "33, R64",
      "64, R64"
  })
  void testFromNbTeams(int teamCount, Stage expectedStage) {
    assertEquals(expectedStage, Stage.fromNbTeams(teamCount));
  }
}