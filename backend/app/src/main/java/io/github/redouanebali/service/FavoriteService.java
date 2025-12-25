package io.github.redouanebali.service;

import io.github.redouanebali.dto.GameTournamentMapping;
import io.github.redouanebali.dto.response.TournamentSummaryDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.User;
import io.github.redouanebali.model.UserFavoriteGame;
import io.github.redouanebali.model.UserFavoriteTournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.repository.UserFavoriteGameRepository;
import io.github.redouanebali.repository.UserFavoriteTournamentRepository;
import io.github.redouanebali.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

  private static final String USER_NOT_FOUND_MESSAGE = "User not found";

  private final UserFavoriteTournamentRepository userFavoriteTournamentRepository;
  private final UserFavoriteGameRepository       userFavoriteGameRepository;
  private final UserRepository                   userRepository;
  private final TournamentRepository             tournamentRepository;
  private final GameRepository                   gameRepository;

  @Transactional
  public void addFavoriteTournament(String userEmail, Long tournamentId) {
    if (userFavoriteTournamentRepository.existsByUserEmailAndTournamentId(userEmail, tournamentId)) {
      return;
    }

    User       user       = getUser(userEmail);
    Tournament tournament = tournamentRepository.getReferenceById(tournamentId);

    UserFavoriteTournament favorite = new UserFavoriteTournament();
    favorite.setUser(user);
    favorite.setTournament(tournament);
    userFavoriteTournamentRepository.save(favorite);
  }

  @Transactional
  public void removeFavoriteTournament(String userEmail, Long tournamentId) {
    userFavoriteTournamentRepository.deleteByUserEmailAndTournamentId(userEmail, tournamentId);
  }

  public List<UserFavoriteTournament> getFavoriteTournaments(String userEmail) {
    User user = getUser(userEmail);
    return userFavoriteTournamentRepository.findByUserOrderByAddedAtDesc(user);
  }

  public boolean isTournamentFavorite(String userEmail, Long tournamentId) {
    return userFavoriteTournamentRepository.existsByUserEmailAndTournamentId(userEmail, tournamentId);
  }

  @Transactional
  public void addFavoriteGame(String userEmail, Long gameId) {
    if (userFavoriteGameRepository.existsByUserEmailAndGameId(userEmail, gameId)) {
      return;
    }

    User user = getUser(userEmail);
    Game game = gameRepository.getReferenceById(gameId);

    UserFavoriteGame favorite = new UserFavoriteGame();
    favorite.setUser(user);
    favorite.setGame(game);
    userFavoriteGameRepository.save(favorite);
  }

  @Transactional
  public void removeFavoriteGame(String userEmail, Long gameId) {
    userFavoriteGameRepository.deleteByUserEmailAndGameId(userEmail, gameId);
  }

  public List<UserFavoriteGame> getFavoriteGames(String userEmail) {
    return userFavoriteGameRepository.findByUserEmailOrderByAddedAtDesc(userEmail);
  }

  public boolean isGameFavorite(String userEmail, Long gameId) {
    return userFavoriteGameRepository.existsByUserEmailAndGameId(userEmail, gameId);
  }

  public Map<Long, TournamentSummaryDTO> getGameToTournamentDTOMap(List<Long> gameIds) {
    if (gameIds.isEmpty()) {
      return Map.of();
    }
    return userFavoriteGameRepository.findGameTournamentMappings(gameIds)
                                     .stream()
                                     .filter(mapping -> mapping.getTournamentId() != null)
                                     .collect(Collectors.toMap(
                                         GameTournamentMapping::getGameId,
                                         this::mapToTournamentSummaryDTO
                                     ));
  }

  private TournamentSummaryDTO mapToTournamentSummaryDTO(GameTournamentMapping mapping) {
    TournamentSummaryDTO dto = new TournamentSummaryDTO();
    dto.setId(mapping.getTournamentId());
    dto.setName(mapping.getTournamentName());
    dto.setCity(mapping.getTournamentCity());
    dto.setClub(mapping.getTournamentClub());
    dto.setLevel(mapping.getTournamentLevel());
    dto.setGender(mapping.getTournamentGender());
    dto.setStartDate(mapping.getTournamentStartDate());
    dto.setEndDate(mapping.getTournamentEndDate());
    dto.setOrganizerName(mapping.getTournamentOrganizerName());
    dto.setFeatured(mapping.isTournamentFeatured());
    return dto;
  }

  private User getUser(String email) {
    return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));
  }

}
