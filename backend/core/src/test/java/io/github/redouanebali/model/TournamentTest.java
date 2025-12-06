package io.github.redouanebali.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TournamentTest {

  @Test
  void getRoundByStage_shouldReturnRoundWhenExists() {
    Tournament tournament = new Tournament();
    Round      round      = new Round(Stage.FINAL);
    tournament.getRounds().add(round);

    Round result = tournament.getRoundByStage(Stage.FINAL);

    assertNotNull(result);
    assertEquals(Stage.FINAL, result.getStage());
  }

  @Test
  void getRoundByStage_shouldThrowWhenNotFound() {
    Tournament tournament = new Tournament();

    assertThrows(IllegalStateException.class,
                 () -> tournament.getRoundByStage(Stage.FINAL));
  }

  @Test
  void getRoundByStage_shouldThrowWithCorrectMessage() {
    Tournament tournament = new Tournament();

    IllegalStateException exception = assertThrows(IllegalStateException.class,
                                                   () -> tournament.getRoundByStage(Stage.QUARTERS));

    assertEquals("No round found for QUARTERS", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
      "owner123, owner123, true",
      "owner123, editor456, false",
      "owner123, '', false",
      "'', owner123, false"
  })
  void isEditableBy_shouldCheckOwnerOnly(String ownerId, String userId, boolean expected) {
    Tournament tournament = new Tournament();
    tournament.setOwnerId(ownerId);

    assertEquals(expected, tournament.isEditableBy(userId));
  }

  @Test
  void isEditableBy_shouldReturnFalseWhenUserIdIsNull() {
    Tournament tournament = new Tournament();
    tournament.setOwnerId("owner123");

    assertFalse(tournament.isEditableBy(null));
  }

  @Test
  void isEditableBy_shouldReturnTrueForEditor() {
    Tournament tournament = new Tournament();
    tournament.setOwnerId("owner123");
    tournament.getEditorIds().add("editor456");

    assertTrue(tournament.isEditableBy("editor456"));
    assertFalse(tournament.isEditableBy("stranger789"));
  }

  @Test
  void isEditableBy_shouldReturnTrueForOwnerEvenWithEditors() {
    Tournament tournament = new Tournament();
    tournament.setOwnerId("owner123");
    tournament.getEditorIds().add("editor456");

    assertTrue(tournament.isEditableBy("owner123"));
  }

  @Test
  void getEditorIds_shouldReturnEmptySetWhenNull() {
    Tournament tournament = new Tournament();
    tournament.setOwnerId("owner123");
    // editorIds could be null in some scenarios

    assertNotNull(tournament.getEditorIds());
    assertTrue(tournament.getEditorIds().isEmpty());
  }

  @Test
  void getEditorIds_shouldReturnModifiableSet() {
    Tournament tournament = new Tournament();

    tournament.getEditorIds().add("editor1");
    tournament.getEditorIds().add("editor2");

    assertEquals(2, tournament.getEditorIds().size());
    assertTrue(tournament.getEditorIds().contains("editor1"));
    assertTrue(tournament.getEditorIds().contains("editor2"));
  }

  @ParameterizedTest
  @CsvSource({
      "owner123, owner123, editor456, true",
      "owner123, editor456, editor456, true",
      "owner123, stranger789, editor456, false",
      "owner123, '', editor456, false"
  })
  void isEditableBy_withMultipleEditors(String ownerId, String userId, String editorId, boolean expected) {
    Tournament tournament = new Tournament();
    tournament.setOwnerId(ownerId);
    tournament.getEditorIds().add(editorId);

    assertEquals(expected, tournament.isEditableBy(userId));
  }

  @Test
  void onUpdate_shouldUpdateTimestamp() {
    Tournament tournament = new Tournament();
    tournament.setOwnerId("owner123");
    tournament.setName("Test Tournament");

    java.time.Instant beforeUpdate = tournament.getUpdatedAt();

    tournament.onUpdate();

    assertTrue(tournament.getUpdatedAt().isAfter(beforeUpdate),
               "updatedAt should be updated after onUpdate call");
  }

  @Test
  void constructor_shouldInitializeCollections() {
    Tournament tournament = new Tournament();

    assertNotNull(tournament.getRounds());
    assertNotNull(tournament.getPlayerPairs());
    assertNotNull(tournament.getEditorIds());
    assertTrue(tournament.getRounds().isEmpty());
    assertTrue(tournament.getPlayerPairs().isEmpty());
    assertTrue(tournament.getEditorIds().isEmpty());
  }
}
