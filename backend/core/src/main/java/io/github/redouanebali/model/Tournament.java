package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.redouanebali.model.format.TournamentConfig;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
  private Long   id;
  // Primary owner (single main owner id, kept for backward compatibility)
  @Column(nullable = false, length = 191)
  private String ownerId;

  // set of editor ids who are allowed to edit tournament (emails or userIds)
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "tournament_editors", joinColumns = @JoinColumn(name = "tournament_id"))
  @Column(name = "editor_id", length = 191)
  private Set<String> editorIds = new HashSet<>();

  @Column(nullable = false, updatable = false)
  private Instant          createdAt   = Instant.now();
  @Column(nullable = false)
  private Instant          updatedAt   = Instant.now();
  @NotBlank
  private String           name;
  @Setter(AccessLevel.NONE)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "tournament_id")
  @OrderColumn(name = "order_index") // persists list order
  private List<Round>      rounds      = new ArrayList<>();
  @Setter(AccessLevel.NONE)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  @OrderColumn(name = "order_index") // persists list order
  private List<PlayerPair> playerPairs = new ArrayList<>();
  @Size(max = 1000)
  private String           description;
  @Size(max = 50)
  private String           city;
  @Size(max = 50)
  private String           club;
  @Size(max = 100)
  private String           organizerName;
  @Column(name = "is_featured")
  private boolean          featured;
  @Enumerated(EnumType.STRING)
  private Gender           gender;
  @Enumerated(EnumType.STRING)
  private TournamentLevel  level;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate        startDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate        endDate;
  // Storage: JSON for config
  @Column(name = "config")
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  private TournamentConfig config;

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }


  public Round getRoundByStage(Stage stage) {
    return this.getRounds().stream()
               .filter(round -> round.getStage() == stage)
               .findFirst()
               .orElseThrow(() -> new IllegalStateException("No round found for " + stage));
  }

  /**
   * Convenience: return the editor ids set (never null)
   */
  public Set<String> getEditorIds() {
    if (editorIds == null) {
      editorIds = new HashSet<>();
    }
    return editorIds;
  }

  /**
   * Return true if userId is the primary owner or included in editorIds. Note: super-admin checks should be performed at service layer.
   */
  public boolean isEditableBy(String userId) {
    if (userId == null) {
      return false;
    }
    if (Objects.equals(this.ownerId, userId)) {
      return true;
    }
    return getEditorIds().contains(userId);
  }

}
