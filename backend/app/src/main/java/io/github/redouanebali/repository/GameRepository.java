package io.github.redouanebali.repository;

import io.github.redouanebali.model.Game;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

  List<Game> findByPoolIsNull();

  List<Game> findByCreatedByAndPoolIsNull(String createdBy);

  List<Game> findByCreatedBy(String createdBy);

}
