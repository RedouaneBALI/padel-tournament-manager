package io.github.redouanebali.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a vote for a team in a game. A voter is identified by either their user email (if authenticated) or a voter fingerprint (if anonymous).
 */
@Entity
@Table(
    name = "votes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "voter_id"}),
    indexes = @Index(columnList = "game_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "game_id", nullable = false)
  private Long gameId;

  /**
   * Unique voter identifier: user email for authenticated users, or IP+fingerprint hash for anonymous users.
   */
  @Column(name = "voter_id", nullable = false)
  private String voterId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TeamSide teamSide;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /**
   * True if the voter was authenticated when voting.
   */
  @Column(name = "is_authenticated")
  private boolean authenticated;

  public Vote(Long gameId, String voterId, TeamSide teamSide, boolean authenticated) {
    this.gameId        = gameId;
    this.voterId       = voterId;
    this.teamSide      = teamSide;
    this.authenticated = authenticated;
    this.createdAt     = Instant.now();
  }
}

