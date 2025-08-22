package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
  private Long                   id;
  // Who can edit this tournament (user email)
  @Column(name = "owner_id", nullable = false, length = 191)
  private String                 ownerId;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant                createdAt   = Instant.now();
  @Column(name = "updated_at", nullable = false)
  private Instant                updatedAt   = Instant.now();
  @NotBlank
  private String                 name;
  @Setter(AccessLevel.NONE)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "tournament_id")
  @OrderColumn(name = "order_index") // persists list order
  private List<Round>            rounds      = new ArrayList<>();
  @Setter(AccessLevel.NONE)
  @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "tournament_id")
  @OrderColumn(name = "order_index") // persists list order
  private List<PlayerPair>       playerPairs = new ArrayList<>();
  @Size(max = 1000)
  private String                 description;
  @Size(max = 50)
  private String                 city;
  @Size(max = 50)
  private String                 club;
  @Enumerated(EnumType.STRING)
  private Gender                 gender;
  @Enumerated(EnumType.STRING)
  private TournamentLevel        level;
  @Enumerated(EnumType.STRING)
  @Column(name = "format")
  private TournamentFormat       format;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "start_date")
  private LocalDate              startDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Column(name = "end_date")
  private LocalDate              endDate;
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "JSONB")
  private TournamentFormatConfig config;
  @JsonProperty("isEditable")
  @Transient
  private boolean                editable;

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }

}
