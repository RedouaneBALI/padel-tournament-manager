package io.github.redouanebali.controller;

import io.github.redouanebali.dto.response.GameSummaryDTO;
import io.github.redouanebali.dto.response.TournamentSummaryDTO;
import io.github.redouanebali.mapper.FavoriteMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.UserFavoriteGame;
import io.github.redouanebali.model.UserFavoriteTournament;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.FavoriteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

  private final FavoriteService favoriteService;
  private final FavoriteMapper  favoriteMapper;

  @GetMapping("/tournaments")
  public ResponseEntity<List<TournamentSummaryDTO>> getFavoriteTournaments() {
    String userEmail = SecurityUtil.currentUserId();
    var    favorites = favoriteService.getFavoriteTournaments(userEmail);
    List<Tournament> tournaments = favorites.stream()
                                            .map(UserFavoriteTournament::getTournament)
                                            .toList();
    return ResponseEntity.ok(favoriteMapper.toTournamentSummaryDTOList(tournaments));
  }

  @PostMapping("/tournaments/{tournamentId}")
  public ResponseEntity<Void> addFavoriteTournament(@PathVariable Long tournamentId) {
    String userEmail = SecurityUtil.currentUserId();
    favoriteService.addFavoriteTournament(userEmail, tournamentId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/tournaments/{tournamentId}")
  public ResponseEntity<Void> removeFavoriteTournament(@PathVariable Long tournamentId) {
    String userEmail = SecurityUtil.currentUserId();
    favoriteService.removeFavoriteTournament(userEmail, tournamentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/games")
  public ResponseEntity<List<GameSummaryDTO>> getFavoriteGames() {
    String userEmail = SecurityUtil.currentUserId();
    var    favorites = favoriteService.getFavoriteGames(userEmail);
    List<Game> games = favorites.stream()
                                .map(UserFavoriteGame::getGame)
                                .toList();
    return ResponseEntity.ok(favoriteMapper.toGameSummaryDTOList(games));
  }

  @PostMapping("/games/{gameId}")
  public ResponseEntity<Void> addFavoriteGame(@PathVariable Long gameId) {
    String userEmail = SecurityUtil.currentUserId();
    favoriteService.addFavoriteGame(userEmail, gameId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/games/{gameId}")
  public ResponseEntity<Void> removeFavoriteGame(@PathVariable Long gameId) {
    String userEmail = SecurityUtil.currentUserId();
    favoriteService.removeFavoriteGame(userEmail, gameId);
    return ResponseEntity.noContent().build();
  }

}

