package io.github.redouanebali.service;

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

    User       user       = userRepository.findByEmail(userEmail).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));
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
    User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));
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

    User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));
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
    User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MESSAGE));
    return userFavoriteGameRepository.findByUserOrderByAddedAtDesc(user);
  }

  public boolean isGameFavorite(String userEmail, Long gameId) {
    return userFavoriteGameRepository.existsByUserEmailAndGameId(userEmail, gameId);
  }

}
