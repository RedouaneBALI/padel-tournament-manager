package io.github.redouanebali.repository;

import io.github.redouanebali.model.User;
import io.github.redouanebali.model.UserFavoriteTournament;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFavoriteTournamentRepository extends JpaRepository<UserFavoriteTournament, Long> {

  List<UserFavoriteTournament> findByUserOrderByAddedAtDesc(User user);

  @Query("SELECT uft FROM UserFavoriteTournament uft WHERE uft.user.email = :email AND uft.tournament.id = :tournamentId")
  Optional<UserFavoriteTournament> findByUserEmailAndTournamentId(@Param("email") String email, @Param("tournamentId") Long tournamentId);

  @Modifying
  @Query("DELETE FROM UserFavoriteTournament uft WHERE uft.user.email = :email AND uft.tournament.id = :tournamentId")
  void deleteByUserEmailAndTournamentId(@Param("email") String email, @Param("tournamentId") Long tournamentId);

  @Query(
      "SELECT CASE WHEN COUNT(uft) > 0 THEN true ELSE false END FROM UserFavoriteTournament uft WHERE uft.user.email = :email AND uft.tournament.id = :tournamentId")
  boolean existsByUserEmailAndTournamentId(@Param("email") String email, @Param("tournamentId") Long tournamentId);

}
