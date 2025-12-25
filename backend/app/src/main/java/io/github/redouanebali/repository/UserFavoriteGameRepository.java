package io.github.redouanebali.repository;

import io.github.redouanebali.dto.GameTournamentIdMapping;
import io.github.redouanebali.dto.GameTournamentMapping;
import io.github.redouanebali.model.User;
import io.github.redouanebali.model.UserFavoriteGame;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFavoriteGameRepository extends JpaRepository<UserFavoriteGame, Long> {

  @Query(
      "SELECT ufg FROM UserFavoriteGame ufg JOIN FETCH ufg.game g LEFT JOIN FETCH g.teamA ta LEFT JOIN FETCH ta.player1 p1a LEFT JOIN FETCH ta.player2 p2a LEFT JOIN FETCH g.teamB tb LEFT JOIN FETCH tb.player1 p1b LEFT JOIN FETCH tb.player2 p2b WHERE ufg.user.email = :email ORDER BY ufg.addedAt DESC")
  List<UserFavoriteGame> findByUserEmailOrderByAddedAtDesc(@Param("email") String email);

  List<UserFavoriteGame> findByUserOrderByAddedAtDesc(User user);

  @Query("SELECT ufg FROM UserFavoriteGame ufg WHERE ufg.user.email = :email AND ufg.game.id = :gameId")
  Optional<UserFavoriteGame> findByUserEmailAndGameId(@Param("email") String email, @Param("gameId") Long gameId);

  @Modifying
  @Query("DELETE FROM UserFavoriteGame ufg WHERE ufg.user.email = :email AND ufg.game.id = :gameId")
  void deleteByUserEmailAndGameId(@Param("email") String email, @Param("gameId") Long gameId);

  @Query("SELECT CASE WHEN COUNT(ufg) > 0 THEN true ELSE false END FROM UserFavoriteGame ufg WHERE ufg.user.email = :email AND ufg.game.id = :gameId")
  boolean existsByUserEmailAndGameId(@Param("email") String email, @Param("gameId") Long gameId);

  @Query(value = "SELECT g.id as gameId, t.id as tournamentId FROM game g LEFT JOIN round r ON g.round_id = r.id LEFT JOIN tournament t ON r.tournament_id = t.id WHERE g.id IN :gameIds",
         nativeQuery = true)
  List<GameTournamentIdMapping> findTournamentIdsByGameIds(@Param("gameIds") List<Long> gameIds);

  @Query(value = "SELECT g.id as gameId, t.id as tournamentId, t.name as tournamentName, t.city as tournamentCity, t.club as tournamentClub, t.level as tournamentLevel, t.gender as tournamentGender, t.start_date as tournamentStartDate, t.end_date as tournamentEndDate, t.organizer_name as tournamentOrganizerName, t.is_featured as tournamentFeatured FROM game g LEFT JOIN round r ON g.round_id = r.id LEFT JOIN tournament t ON r.tournament_id = t.id WHERE g.id IN :gameIds",
         nativeQuery = true)
  List<GameTournamentMapping> findGameTournamentMappings(@Param("gameIds") List<Long> gameIds);

}
