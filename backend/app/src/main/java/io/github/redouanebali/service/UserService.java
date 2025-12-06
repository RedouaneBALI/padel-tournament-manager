package io.github.redouanebali.service;

import io.github.redouanebali.model.User;
import io.github.redouanebali.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public User getOrCreateUser(Map<String, Object> claims) {
    String         email    = (String) claims.get("email");
    Optional<User> existing = userRepository.findByEmail(email);
    if (existing.isPresent()) {
      // Enrichir si nécessaire (ex. mettre à jour le nom si changé)
      User user = existing.get();
      if (claims.containsKey("name")) {
        user.setName((String) claims.get("name"));
      }
      if (claims.containsKey("locale")) {
        user.setLocale((String) claims.get("locale"));
      }
      return userRepository.save(user);
    } else {
      return userRepository.save(new User(email,
                                          (String) claims.get("name"),
                                          (String) claims.get("locale")));
    }
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public User getUserIfExists(String email) {
    return userRepository.findByEmail(email).orElse(null);
  }

  @Transactional
  public User updateProfile(String email, String name, String locale, User.ProfileType profileType, String city, String country) {
    User user = userRepository.findByEmail(email).orElseGet(() -> {
      User newUser = new User(email, name, locale);
      newUser.setProfileType(profileType);
      newUser.setCity(city);
      newUser.setCountry(country);
      return userRepository.save(newUser);
    });
    user.setName(name);
    user.setLocale(locale);
    user.setProfileType(profileType);
    user.setCity(city);
    user.setCountry(country);
    return userRepository.save(user);
  }
}
