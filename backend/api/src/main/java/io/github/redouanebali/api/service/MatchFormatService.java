package io.github.redouanebali.api.service;

import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchFormatService {

  private final TournamentRepository tournamentRepository;
  private final SecurityProps        securityProps;

  /**
   * Retrieves the match format for a specific tournament round/stage.
   *
   * @param tournamentId the tournament ID
   * @param stage the tournament stage to get the match format from
   * @return the match format for the specified stage
   * @throws IllegalArgumentException if tournament or stage round is not found
   */
  public MatchFormat getMatchFormatForRound(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);
    return tournament.getRounds().stream()
                     .filter(round -> round.getStage() == stage)
                     .findFirst()
                     .map(Round::getMatchFormat)
                     .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
  }

  /**
   * Updates the match format for a specific tournament round/stage. Only the owner or super admins can modify match formats. Creates a new format if
   * none exists, otherwise updates the existing one.
   *
   * @param tournamentId the tournament ID
   * @param stage the tournament stage to update
   * @param newFormat the new match format configuration
   * @return the updated match format
   * @throws IllegalArgumentException if tournament or stage round is not found
   * @throws AccessDeniedException if user lacks modification rights
   */
  public MatchFormat updateMatchFormatForRound(Long tournamentId, Stage stage, MatchFormat newFormat) {
    Tournament tournament = getTournamentById(tournamentId);

    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to edit the match format for this tournament");
    }

    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
    MatchFormat currentFormat = round.getMatchFormat();

    if (currentFormat == null) {
      round.setMatchFormat(newFormat);
    } else {
      currentFormat.setNumberOfSetsToWin(newFormat.getNumberOfSetsToWin());
      currentFormat.setGamesPerSet(newFormat.getGamesPerSet());
      currentFormat.setAdvantage(newFormat.isAdvantage());
      currentFormat.setSuperTieBreakInFinalSet(newFormat.isSuperTieBreakInFinalSet());
    }

    tournamentRepository.save(tournament);
    return newFormat;
  }

  /**
   * Retrieves a tournament by its ID. This is a helper method used internally by the io.github.redouanebali.api.service.
   *
   * @param id the tournament ID
   * @return the tournament entity
   * @throws IllegalArgumentException if tournament is not found
   */
  private Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }
}
