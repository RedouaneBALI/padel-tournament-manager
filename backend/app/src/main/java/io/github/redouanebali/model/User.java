package io.github.redouanebali.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

  private static final String      DEFAULT_LOCALE = "fr";
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private              Long        id;
  @Column(unique = true, nullable = false)
  @NotNull
  @Email
  private              String      email;
  private              String      name;
  private              String      locale;
  @Enumerated(EnumType.STRING)
  private              ProfileType profileType;

  private String city;
  private String country;

  public User(String email, String name, String locale) {
    this.email  = email;
    this.name   = name;
    this.locale = locale != null ? locale : DEFAULT_LOCALE;
  }

  public enum ProfileType {
    PLAYER, SPECTATOR, ORGANIZER
  }
}
