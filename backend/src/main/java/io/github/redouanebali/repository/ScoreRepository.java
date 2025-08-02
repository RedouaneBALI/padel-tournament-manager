package io.github.redouanebali.repository;

import io.github.redouanebali.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

//  List<Game> findByTournamentId(Long tournamentId);
}
