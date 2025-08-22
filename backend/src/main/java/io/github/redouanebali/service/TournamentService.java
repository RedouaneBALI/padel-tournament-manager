package io.github.redouanebali.service;

import io.github.redouanebali.dto.UpdateTournamentRequest;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.builder.TournamentRoundBuilder;
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

  private final TournamentRoundBuilder roundBuilder;

  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }


  @Transactional
  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    if (tournament.getFormat() != null && tournament.getConfig() != null) {
      roundBuilder.validateAndBuild(tournament);
    }
    tournament.setOwnerId(SecurityUtil.currentUserId());

    Tournament savedTournament = tournamentRepository.save(tournament);
    log.info("Created tournament with id {}", savedTournament.getId());
    return applyEditable(savedTournament);
  }

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
      roundBuilder.validateAndBuild(existing);
    }

    return applyEditable(tournamentRepository.save(existing));
  }

  /**
   * Call generator.generate() and dispatch all the players into games from the created round
   *
   * @param tournamentId the id of the tournament
   * @param manual flag indicating manual draw generation
   * @return the new Tournament
   */
  public Tournament generateDraw(Long tournamentId, boolean manual) {
    Tournament  tournament  = getTournamentById(tournamentId);
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (!superAdmins.contains(me) && !me.equals(tournament.getOwnerId())) {
      throw new AccessDeniedException("You are not allowed to generate the draw for this tournament");
    }
    return applyEditable(drawGenerationService.generateDraw(tournament, manual));
  }

  public Set<Game> getGamesByTournamentAndStage(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);

    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));

    return new LinkedHashSet<>(round.getGames());
  }

  public Tournament getTournamentForCurrentUser(Long id) {
    return applyEditable(getTournamentById(id));
  }

  private Tournament applyEditable(Tournament t) {
    String      me          = SecurityUtil.currentUserId();
    Set<String> superAdmins = securityProps.getSuperAdmins();
    t.setEditable(superAdmins.contains(me) || me.equals(t.getOwnerId()));
    return t;
  }

  public List<Tournament> listByOwner(String ownerId) {
    return tournamentRepository.findAllByOwnerId(ownerId);
  }

  public List<Tournament> listAll() {
    return tournamentRepository.findAll();
  }
}
