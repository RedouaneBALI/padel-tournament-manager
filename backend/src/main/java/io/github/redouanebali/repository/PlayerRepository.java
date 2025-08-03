package io.github.redouanebali.repository;

import io.github.redouanebali.model.Player;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

  Optional<Player> findByName(String name);


}
