package io.github.redouanebali.repository;

import io.github.redouanebali.model.Game;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Game entities. Handles persistence operations for standalone games (matches without tournaments).
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

  /**
   * Finds all standalone games (games not associated with any pool/tournament).
   *
   * @return list of standalone games
   */
  List<Game> findByPoolIsNull();
}

