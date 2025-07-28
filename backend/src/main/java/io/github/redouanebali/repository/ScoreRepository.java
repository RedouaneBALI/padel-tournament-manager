package io.github.redouanebali.repository;

import io.github.redouanebali.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

//  List<Game> findByTournamentId(Long tournamentId);
}
