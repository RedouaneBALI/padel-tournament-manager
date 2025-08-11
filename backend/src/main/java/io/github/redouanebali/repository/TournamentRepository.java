package io.github.redouanebali.repository;

import io.github.redouanebali.model.Tournament;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

  Optional<Tournament> findByIdAndOwnerId(Long id, String ownerId);

  Page<Tournament> findAllByOwnerId(String ownerId, Pageable pageable);
}
