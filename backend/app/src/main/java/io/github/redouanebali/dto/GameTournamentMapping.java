package io.github.redouanebali.dto;

import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.TournamentLevel;
import java.time.LocalDate;

public interface GameTournamentMapping {

  Long getGameId();

  Long getTournamentId();

  String getTournamentName();

  String getTournamentCity();

  String getTournamentClub();

  TournamentLevel getTournamentLevel();

  Gender getTournamentGender();

  LocalDate getTournamentStartDate();

  LocalDate getTournamentEndDate();

  String getTournamentOrganizerName();

  boolean isTournamentFeatured();
}

