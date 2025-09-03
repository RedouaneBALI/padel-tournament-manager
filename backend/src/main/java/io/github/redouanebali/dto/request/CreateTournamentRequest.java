package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/**
 * Request DTO for creating a new tournament. Contains only the fields that can be set during tournament creation.
 */
@Data
public class CreateTournamentRequest {

  @NotBlank(message = "Tournament name is required")
  @Size(max = 200, message = "Tournament name must not exceed 200 characters")
  private String name;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  @FutureOrPresent(message = "Start date must be today or in the future")
  private LocalDate startDate;

  private LocalDate endDate;

  @Size(max = 100, message = "City name must not exceed 100 characters")
  private String city;

  @Size(max = 200, message = "Club name must not exceed 200 characters")
  private String club;

  @Size(max = 50, message = "Gender must not exceed 50 characters")
  private String gender;

  @Size(max = 50, message = "Level must not exceed 50 characters")
  private String level;

  @NotNull(message = "Tournament format is required")
  private TournamentFormat format;

  @Valid
  private TournamentFormatConfig config;
}
