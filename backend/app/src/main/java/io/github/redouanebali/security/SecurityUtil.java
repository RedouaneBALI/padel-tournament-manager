package io.github.redouanebali.security;

import io.github.redouanebali.model.User;
import io.github.redouanebali.service.UserService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtil {

  private static final String EMAIL_CLAIM = "email";

  private static UserService userService; // Inject via setter

  private SecurityUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static void setUserService(UserService service) {
    userService = service;
  }

  // Return the stable user id we use as ownerId (e.g., the JWT "sub")
  public static String currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof final JwtAuthenticationToken jwtAuth)) {
      // Covers null, anonymous, or any non-JWT authentication
      return null;
    }
    return jwtAuth.getToken().getClaimAsString(EMAIL_CLAIM);
  }

  public static boolean isSuperAdmin(Set<String> superAdmins) {
    String me = currentUserId();
    return superAdmins.contains(me);
  }

  public static User getCurrentUser() {
    Map<String, Object> claims = getUserClaims();
    if (claims.isEmpty()) {
      return null;
    }
    return userService.getOrCreateUser(claims);
  }

  public static User getExistingUser() {
    String email = currentUserId();
    if (email == null) {
      return null;
    }
    return userService.getUserIfExists(email);
  }

  public static Map<String, Object> getUserClaims() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
      return Map.of();
    }
    Map<String, Object> claims = new HashMap<>();
    String              email  = jwtAuth.getToken().getClaimAsString(EMAIL_CLAIM);
    if (email != null) {
      claims.put(EMAIL_CLAIM, email);
    }
    String name = jwtAuth.getToken().getClaimAsString("name");
    if (name != null) {
      claims.put("name", name);
    }
    String locale = jwtAuth.getToken().getClaimAsString("locale");
    if (locale != null) {
      claims.put("locale", locale);
    }
    return claims;
  }
}