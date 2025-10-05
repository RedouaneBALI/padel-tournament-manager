package io.github.redouanebali.repository;

import io.github.redouanebali.model.PlayerPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerPairRepository extends JpaRepository<PlayerPair, Long> {

}
