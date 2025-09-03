package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.dto.request.UpdateTournamentRequest;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

  private final TournamentRepository tournamentRepository;
  private final SecurityProps        securityProps;

  private final DrawGenerationService drawGenerationService;

  /**
   * Retrieves a tournament by its ID.
   *
   * @param id the tournament ID
   * @return the tournament entity
   * @throws IllegalArgumentException if the tournament is not found
   */
  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }

  /**
   * Creates a new tournament and validates its configuration. If format and config are provided, validates the tournament structure. Sets the current
   * user as the tournament owner.
   *
   * @param tournament the tournament to create
   * @return the created tournament with editable flag set
   * @throws IllegalArgumentException if tournament is null or configuration is invalid
   */
  @Transactional
  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    if (tournament.getFormat() != null && tournament.getConfig() != null) {
      drawGenerationService.validate(tournament);
    }
    tournament.setOwnerId(SecurityUtil.currentUserId());

    Tournament savedTournament = tournamentRepository.save(tournament);
    log.info("Created tournament with id {}", savedTournament.getId());
    return applyEditable(savedTournament);
  }

  /**
   * Deletes a tournament if the current user is the owner or a super admin.
   *
   * @param tournamentId the ID of the tournament to delete
   * @throws IllegalArgumentException if tournament is not found
   * @throws AccessDeniedException if user lacks deletion rights
   */
  @Transactional
  public void deleteTournament(Long tournamentId) {
    Tournament  existing    = getTournamentById(tournamentId);
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(existing.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to delete this tournament");
    }
    tournamentRepository.delete(existing);
    log.info("Deleted tournament with id {}", tournamentId);
  }

  /**
   * Updates an existing tournament with new information. Only the owner or super admins can update tournaments. Rebuilds tournament structure if
   * format and config are provided.
   *
   * @param tournamentId the ID of the tournament to update
   * @param updatedTournament the new tournament data
   * @return the updated tournament with editable flag set
   * @throws IllegalArgumentException if tournament is not found
   * @throws AccessDeniedException if user lacks update rights
   */
  @Transactional
  public Tournament updateTournament(Long tournamentId, UpdateTournamentRequest updatedTournament) {
    Tournament  existing    = getTournamentById(tournamentId);
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(existing.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to edit this tournament");
    }
    existing.setName(updatedTournament.getName());
    existing.setStartDate(updatedTournament.getStartDate());
    existing.setEndDate(updatedTournament.getEndDate());
    existing.setDescription(updatedTournament.getDescription());
    existing.setCity(updatedTournament.getCity());
    existing.setClub(updatedTournament.getClub());
    existing.setGender(updatedTournament.getGender());
    existing.setLevel(updatedTournament.getLevel());
    existing.setFormat(updatedTournament.getFormat());
    existing.setConfig(updatedTournament.getConfig());
    // Rebuild initial rounds if we have enough info (format + config) and a meaningful draw size
    if (existing.getFormat() != null
        && existing.getConfig() != null) {
      // Validate and (re)build the structure for the new/updated format configuration
      TournamentFormat format = existing.getFormat();
      existing.setFormat(format);
      TournamentFormatConfig config = existing.getConfig();
      // roundBuilder.validateAndBuild(existing);
    }

    return applyEditable(tournamentRepository.save(existing));
  }

  /**
   * Generates an automatic draw using seeding algorithm. Places teams based on their seeds and fills remaining positions randomly. Only the owner or
   * super admins can generate draws.
   *
   * @param tournamentId the tournament ID
   * @return the tournament with generated draw and editable flag set
   * @throws IllegalArgumentException if tournament is not found
   * @throws AccessDeniedException if user lacks generation rights
   */
  public Tournament generateDrawAuto(Long tournamentId) {
    Tournament  tournament  = getTournamentById(tournamentId);
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to generate the draw for this tournament");
    }
    return applyEditable(drawGenerationService.generateDrawAuto(tournament));
  }

  /**
   * Generates a manual draw using user-provided initial rounds configuration. Replaces existing rounds with the provided structure. Only the owner or
   * super admins can generate draws.
   *
   * @param tournamentId the tournament ID
   * @param initialRounds optional list of initial rounds provided by user
   * @return the tournament with generated draw and editable flag set
   * @throws IllegalArgumentException if tournament is not found
   * @throws AccessDeniedException if user lacks generation rights
   */
  @Transactional
  public Tournament generateDrawManual(Long tournamentId, List<RoundRequest> initialRounds) {
    Tournament  tournament  = getTournamentById(tournamentId);
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to generate the draw for this tournament");
    }
    return applyEditable(drawGenerationService.generateDrawManual(tournament, initialRounds));
  }

  /**
   * Retrieves all games for a specific tournament stage.
   *
   * @param tournamentId the tournament ID
   * @param stage the tournament stage to get games from
   * @return set of games for the specified stage
   * @throws IllegalArgumentException if tournament or stage round is not found
   */
  public Set<Game> getGamesByTournamentAndStage(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);

    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));

    return new LinkedHashSet<>(round.getGames());
  }

  /**
   * Retrieves a tournament for the current user with editable flag properly set.
   *
   * @param id the tournament ID
   * @return the tournament with editable flag indicating if current user can modify it
   * @throws IllegalArgumentException if tournament is not found
   */
  public Tournament getTournamentForCurrentUser(Long id) {
    return applyEditable(getTournamentById(id));
  }

  /**
   * Sets the editable flag based on current user's ownership rights. Tournament is editable if user is owner or super admin.
   *
   * @param t the tournament to process
   * @return the tournament with updated editable flag
   */
  private Tournament applyEditable(Tournament t) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    t.setEditable(superAdmins.contains(me) || me.equals(t.getOwnerId()));
    return t;
  }

  /**
   * Retrieves all tournaments owned by a specific user.
   *
   * @param ownerId the owner's user ID
   * @return list of tournaments owned by the user
   */
  public List<Tournament> listByOwner(String ownerId) {
    return tournamentRepository.findAllByOwnerId(ownerId);
  }

  /**
   * Retrieves all tournaments in the system. Typically used by super admins.
   *
   * @return list of all tournaments
   */
  public List<Tournament> listAll() {
    return tournamentRepository.findAll();
  }
}
