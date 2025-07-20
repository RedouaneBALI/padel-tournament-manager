package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
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
  private Integer birthYear;

}
