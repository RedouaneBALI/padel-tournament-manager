package io.github.redouanebali.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
  private Long id;
  private String name;
  private Integer ranking;
  private Integer points;
  @JsonProperty("birth_year")
  private Integer birthYear;

}
