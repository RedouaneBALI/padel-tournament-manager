package io.github.redouanebali.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for updating an existing tournament. Extends BaseTournamentRequest with update-specific validations. Whitelisted fields that are
 * allowed to be updated on a Tournament. Keep this DTO minimal; pairs, rounds, and scores have their own endpoints.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
public class UpdateTournamentRequest extends BaseTournamentRequest {

  /**
   * Tournament name is required for updates.
   */
  @NotBlank(message = "Tournament name is required")
  @Size(max = 255, message = "Tournament name must not exceed 255 characters")
  @Override
  public String getName() {
    return super.getName();
  }

  // Note: Pas de @FutureOrPresent sur startDate pour les mises à jour
  // car on peut modifier des tournois existants même s'ils ont commencé
}
