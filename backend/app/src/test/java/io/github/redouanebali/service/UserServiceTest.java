package io.github.redouanebali.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.redouanebali.model.User;
import io.github.redouanebali.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @ParameterizedTest
  @CsvSource({
      "test@example.com, John Doe, en",
      "new@example.com, Jane Smith, fr"
  })
  void testGetOrCreateUser(String email, String name, String locale) {
    Map<String, Object> claims = Map.of("email", email, "name", name, "locale", locale);
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User user = userService.getOrCreateUser(claims);
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getName()).isEqualTo(name);
  }

  @Test
  void testUpdateProfile() {
    String email = "test@example.com";
    User   user  = new User(email, "Name", "en");
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User updated = userService.updateProfile(email, "New Name", "fr", User.ProfileType.ORGANIZER, "Paris", "France");
    assertThat(updated.getProfileType()).isEqualTo(User.ProfileType.ORGANIZER);
    assertThat(updated.getCity()).isEqualTo("Paris");
    assertThat(updated.getCountry()).isEqualTo("France");
    assertThat(updated.getName()).isEqualTo("New Name");
    assertThat(updated.getLocale()).isEqualTo("fr");
  }

  @Test
  void testUpdateProfileThrowsExceptionWhenUserNotFound() {
    String email = "nonexistent@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
      userService.updateProfile(email, "New Name", "fr", User.ProfileType.ORGANIZER, "Paris", "France");
    });
  }

  @Test
  void testUpdateOrCreateProfileCreatesIfNotExists() {
    String email = "new@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User updated = userService.updateOrCreateProfile(email, "New Name", "fr", User.ProfileType.PLAYER, "Paris", "France");
    assertThat(updated.getEmail()).isEqualTo(email);
    assertThat(updated.getName()).isEqualTo("New Name");
    assertThat(updated.getLocale()).isEqualTo("fr");
    assertThat(updated.getProfileType()).isEqualTo(User.ProfileType.PLAYER);
    assertThat(updated.getCity()).isEqualTo("Paris");
    assertThat(updated.getCountry()).isEqualTo("France");
  }

  @Test
  void testUpdateOrCreateProfileUpdatesIfExists() {
    String email = "existing@example.com";
    User   user  = new User(email, "Old Name", "en");
    user.setProfileType(User.ProfileType.SPECTATOR);
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User updated = userService.updateOrCreateProfile(email, "Updated Name", "fr", User.ProfileType.ORGANIZER, "Lyon", "France");
    assertThat(updated.getEmail()).isEqualTo(email);
    assertThat(updated.getName()).isEqualTo("Updated Name");
    assertThat(updated.getLocale()).isEqualTo("fr");
    assertThat(updated.getProfileType()).isEqualTo(User.ProfileType.ORGANIZER);
    assertThat(updated.getCity()).isEqualTo("Lyon");
    assertThat(updated.getCountry()).isEqualTo("France");
  }

  @ParameterizedTest
  @CsvSource({
      "test@example.com, John Doe, en, PLAYER, Paris, France",
      "new@example.com, Jane Smith, fr, ORGANIZER, London, UK",
      "admin@example.com, Admin User, es, SPECTATOR, Madrid, Spain"
  })
  void testUpdateOrCreateProfileWithVariousInputs(String email,
                                                  String name,
                                                  String locale,
                                                  User.ProfileType profileType,
                                                  String city,
                                                  String country) {
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = userService.updateOrCreateProfile(email, name, locale, profileType, city, country);
    assertThat(result.getEmail()).isEqualTo(email);
    assertThat(result.getName()).isEqualTo(name);
    assertThat(result.getLocale()).isEqualTo(locale);
    assertThat(result.getProfileType()).isEqualTo(profileType);
    assertThat(result.getCity()).isEqualTo(city);
    assertThat(result.getCountry()).isEqualTo(country);
  }

  @Test
  void testUpdateOrCreateProfileWithMinimalInfo() {
    String email = "minimal@example.com";
    String name  = "Minimal User";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result = userService.updateOrCreateProfile(email, name, null, User.ProfileType.SPECTATOR, null, null);
    assertThat(result.getEmail()).isEqualTo(email);
    assertThat(result.getName()).isEqualTo(name);
    assertThat(result.getProfileType()).isEqualTo(User.ProfileType.SPECTATOR);
    assertThat(result.getLocale()).isNull();
    assertThat(result.getCity()).isNull();
    assertThat(result.getCountry()).isNull();
  }

  @Test
  void testGetUserIfExists() {
    String email = "test@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User(email, "Name", "en")));
    User user = userService.getUserIfExists(email);
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo(email);
  }

  @Test
  void testGetUserIfExistsNotFound() {
    String email = "notfound@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    User user = userService.getUserIfExists(email);
    assertThat(user).isNull();
  }

  @Test
  void testGetUserNameByEmail() {
    String email = "test@example.com";
    String name  = "Test User";
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User(email, name, "en")));
    String result = userService.getUserNameByEmail(email);
    assertThat(result).isEqualTo(name);
  }

  @Test
  void testGetUserNameByEmailNotFound() {
    String email = "notfound@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    String result = userService.getUserNameByEmail(email);
    assertThat(result).isNull();
  }
}
