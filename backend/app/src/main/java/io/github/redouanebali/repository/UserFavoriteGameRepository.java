package io.github.redouanebali.repository;

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

  List<UserFavoriteGame> findByUserOrderByAddedAtDesc(User user);

  @Query("SELECT ufg FROM UserFavoriteGame ufg WHERE ufg.user.email = :email AND ufg.game.id = :gameId")
  Optional<UserFavoriteGame> findByUserEmailAndGameId(@Param("email") String email, @Param("gameId") Long gameId);

  @Modifying
  @Query("DELETE FROM UserFavoriteGame ufg WHERE ufg.user.email = :email AND ufg.game.id = :gameId")
  void deleteByUserEmailAndGameId(@Param("email") String email, @Param("gameId") Long gameId);

  @Query("SELECT CASE WHEN COUNT(ufg) > 0 THEN true ELSE false END FROM UserFavoriteGame ufg WHERE ufg.user.email = :email AND ufg.game.id = :gameId")
  boolean existsByUserEmailAndGameId(@Param("email") String email, @Param("gameId") Long gameId);

}
