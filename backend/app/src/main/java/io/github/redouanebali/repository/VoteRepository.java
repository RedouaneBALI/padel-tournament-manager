package io.github.redouanebali.repository;

import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Vote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

  Optional<Vote> findByGameIdAndVoterId(Long gameId, String voterId);

  boolean existsByGameIdAndVoterId(Long gameId, String voterId);

  long countByGameIdAndTeamSide(Long gameId, TeamSide teamSide);

  @Query("SELECT COUNT(v) FROM Vote v WHERE v.gameId = :gameId AND v.teamSide = 'TEAM_A'")
  long countTeamAVotes(@Param("gameId") Long gameId);

  @Query("SELECT COUNT(v) FROM Vote v WHERE v.gameId = :gameId AND v.teamSide = 'TEAM_B'")
  long countTeamBVotes(@Param("gameId") Long gameId);
}

