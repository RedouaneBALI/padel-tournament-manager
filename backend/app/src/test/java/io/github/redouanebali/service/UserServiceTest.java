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
  void testUpdateProfileCreatesIfNotExists() {
    String email = "new@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User updated = userService.updateProfile(email, "New Name", "fr", User.ProfileType.PLAYER, "Paris", "France");
    assertThat(updated.getEmail()).isEqualTo(email);
    assertThat(updated.getName()).isEqualTo("New Name");
    assertThat(updated.getLocale()).isEqualTo("fr");
    assertThat(updated.getProfileType()).isEqualTo(User.ProfileType.PLAYER);
    assertThat(updated.getCity()).isEqualTo("Paris");
    assertThat(updated.getCountry()).isEqualTo("France");
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
