package io.github.redouanebali.service;

import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.dto.request.UpdateTournamentRequest;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
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
   * Checks if the current user can edit the given tournament.
   */
  private boolean canEditTournament(Tournament tournament) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    return superAdmins.contains(me) || me.equals(tournament.getOwnerId());
  }

  /**
   * Creates a new tournament and validates its configuration. If format and config are provided, validates the tournament structure. Sets the current
   * user as the tournament owner.
   *
   * @param tournament the tournament to create
   * @return the created tournament
   * @throws IllegalArgumentException if tournament is null or configuration is invalid
   */
  @Transactional
  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    if (tournament.getConfig() != null && tournament.getConfig().getFormat() != null) {
      drawGenerationService.validate(tournament);
      drawGenerationService.initializeStructure(tournament);
    }
    tournament.setOwnerId(SecurityUtil.currentUserId());

    Tournament savedTournament = tournamentRepository.save(tournament);
    log.info("Created tournament with id {}", savedTournament.getId());

    return savedTournament;
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
    Tournament existing = getTournamentById(tournamentId);

    if (!canEditTournament(existing)) {
      throw new AccessDeniedException("You are not allowed to delete this tournament");
    }

    tournamentRepository.delete(existing);
    log.info("Deleted tournament with id {}", tournamentId);
  }

  /**
   * Updates an existing tournament with new information. Only the owner or super admins can update tournaments.
   */
  @Transactional
  public Tournament updateTournament(Long tournamentId, UpdateTournamentRequest updatedTournament) {
    Tournament existing = getTournamentById(tournamentId);

    if (!canEditTournament(existing)) {
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
    existing.setConfig(updatedTournament.getConfig());

    return tournamentRepository.save(existing);
  }

  /**
   * Generates an automatic draw using seeding algorithm. Only the owner or super admins can generate draws.
   */
  public Tournament generateDrawAuto(Long tournamentId) {
    Tournament tournament = getTournamentById(tournamentId);

    if (!canEditTournament(tournament)) {
      throw new AccessDeniedException("You are not allowed to generate the draw for this tournament");
    }

    return drawGenerationService.generateDrawAuto(tournament);
  }

  /**
   * Generates a manual draw using user-provided initial rounds configuration. Only the owner or super admins can generate draws.
   */
  @Transactional
  public Tournament generateDrawManual(Long tournamentId, List<RoundRequest> initialRounds) {
    Tournament tournament = getTournamentById(tournamentId);

    if (!canEditTournament(tournament)) {
      throw new AccessDeniedException("You are not allowed to generate the draw for this tournament");
    }

    return drawGenerationService.generateDrawManual(tournament, initialRounds);
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
   * Retrieves a tournament for the current user. The editable flag is calculated dynamically by the TournamentMapper.
   *
   * @param id the tournament ID
   * @return the tournament
   * @throws IllegalArgumentException if tournament is not found
   */
  public Tournament getTournamentForCurrentUser(Long id) {
    return getTournamentById(id);
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
