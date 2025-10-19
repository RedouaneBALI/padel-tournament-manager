package io.github.redouanebali.repository;

import io.github.redouanebali.model.Tournament;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

  Optional<Tournament> findByIdAndOwnerId(Long id, String ownerId);

  List<Tournament> findAllByOwnerId(String ownerId);

  Page<Tournament> findAllByOwnerId(String ownerId, Pageable pageable);

  @Query("SELECT t FROM Tournament t WHERE t.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Tournament> findByIdWithLock(@Param("id") Long id);

  @Query("SELECT t FROM Tournament t WHERE t.id = :id AND t.ownerId = :ownerId")
  Optional<Tournament> findByIdAndOwnerIdWithLock(@Param("id") Long id, @Param("ownerId") String ownerId);
}
