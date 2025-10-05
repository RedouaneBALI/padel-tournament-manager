package io.github.redouanebali.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.TournamentLevel;
import io.github.redouanebali.model.format.TournamentConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/**
 * Base class containing common tournament fields shared between create and update requests. This reduces duplication and ensures consistency across
 * tournament operations.
 */
@Data
public abstract class BaseTournamentRequest {

  /**
   * Tournament name.
   */
  @Size(max = 200, message = "Tournament name must not exceed 200 characters")
  private String name;

  /**
   * Optional long description, shown on overview pages.
   */
  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  /**
   * Optional city and club; used for filtering and display.
   */
  @Size(max = 100, message = "City name must not exceed 100 characters")
  private String city;

  @Size(max = 200, message = "Club name must not exceed 200 characters")
  private String club;

  /**
   * Classification fields.
   */
  private Gender          gender;
  private TournamentLevel level;

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
  @Valid
  private TournamentConfig config;
}
