package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tournament", indexes = {
    @Index(name = "idx_tournament_owner", columnList = "owner_id")
})
public class Tournament {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long             id;
  // Who can edit this tournament (user external id or email)
  @Column(name = "owner_id", nullable = false, length = 191)
  private String           ownerId;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant          createdAt   = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant          updatedAt   = Instant.now();
  private String           name;
  @Setter(AccessLevel.NONE)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @OneToMany(cascade = CascadeType.ALL/*, orphanRemoval = true*/)
  @JoinColumn(name = "tournament_id")
  private List<Round>      rounds      = new ArrayList<>();
  @Setter(AccessLevel.NONE)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  private List<PlayerPair> playerPairs = new ArrayList<>();
  private String           description;
  private String           city;
  private String           club;
  private Gender           gender;
  private TournamentLevel  level;
  private TournamentFormat tournamentFormat;
  private int              nbSeeds;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate        startDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate        endDate;
  private int              nbMaxPairs;
  // for group stage
  private int              nbPools;
  private int              nbPairsPerPool;
  private int              nbQualifiedByPool;
  @JsonProperty("isEditable")
  @Transient
  private boolean          editable;

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
