package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.TournamentLevel;
import io.github.redouanebali.model.format.TournamentConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for creating a new tournament. Extends BaseTournamentRequest with creation-specific validations.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateTournamentRequest extends BaseTournamentRequest {

  /**
   * Tournament name is required for creation.
   */
  @NotBlank(message = "Tournament name is required")
  @Size(max = 200, message = "Tournament name must not exceed 200 characters")
  @Override
  public String getName() {
    return super.getName();
  }

  /**
   * Tournament description, optional for creation.
   */
  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Override
  public String getDescription() {
    return super.getDescription();
  }

  /**
   * Tournament start date must be today or in the future for new tournaments.
   */
  @FutureOrPresent(message = "Start date must be today or in the future")
  @Override
  public LocalDate getStartDate() {
    return super.getStartDate();
  }

  /**
   * Tournament end date, optional and must be after the start date if provided.
   */
  @Override
  public LocalDate getEndDate() {
    return super.getEndDate();
  }

  /**
   * City where the tournament will be held, optional.
   */
  @Size(max = 100, message = "City name must not exceed 100 characters")
  @Override
  public String getCity() {
    return super.getCity();
  }

  /**
   * Club organizing the tournament, optional.
   */
  @Size(max = 200, message = "Club name must not exceed 200 characters")
  @Override
  public String getClub() {
    return super.getClub();
  }

  /**
   * Gender category for the tournament, optional.
   */
  @Override
  public Gender getGender() {
    return super.getGender();
  }

  /**
   * Skill level for the tournament, optional.
   */
  @Override
  public TournamentLevel getLevel() {
    return super.getLevel();
  }

  /**
   * Configuration for the tournament format, optional.
   */
  @Valid
  @Override
  public TournamentConfig getConfig() {
    return super.getConfig();
  }
}
