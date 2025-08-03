package io.github.redouanebali.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Group {

  @ManyToMany
  private final Set<PlayerPair> pairs = new LinkedHashSet<>();
  @Id
  @GeneratedValue
  private       Long            id;
  private       String          name;

}