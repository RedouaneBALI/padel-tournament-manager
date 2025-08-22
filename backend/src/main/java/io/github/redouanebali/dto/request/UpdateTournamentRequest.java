package io.github.redouanebali.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.TournamentLevel;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Whitelisted fields that are allowed to be updated on a Tournament. Keep this DTO minimal; pairs, rounds, and scores have their own endpoints.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTournamentRequest {

  /**
   * Public tournament name.
   */
  @NotBlank
  @Size(max = 255)
  private String name;

  /**
   * Optional long description, shown on overview pages.
   */
  @Size(max = 1000)
  private String description;

  /**
   * Optional city and club; used for filtering and display.
   */
  @Size(max = 50)
  private String city;

  @Size(max = 50)
  private String club;

  /**
   * Classification fields.
   */
  private Gender           gender;
  private TournamentLevel  level;
  private TournamentFormat format;

  /**
   * Event dates (yyyy-MM-dd).
   */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  /**
   * Flexible tournament configuration (stored as JSONB in DB). Example: nbSeeds, groupsKo, qualifyMain sizes, etc.
   */
  private TournamentFormatConfig config;
}
