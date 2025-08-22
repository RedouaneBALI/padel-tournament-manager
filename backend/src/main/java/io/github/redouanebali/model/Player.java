package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Player {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long    id;
  private String  name;
  private Integer ranking;
  private Integer points;
  @JsonProperty("birth_year")
  @Column(name = "birth_year")
  private Integer birthYear;

  public Player(String name) {
    this.name = name;
  }
}
