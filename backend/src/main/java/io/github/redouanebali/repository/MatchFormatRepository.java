package io.github.redouanebali.repository;

import io.github.redouanebali.model.MatchFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchFormatRepository extends JpaRepository<MatchFormat, Long> {

}