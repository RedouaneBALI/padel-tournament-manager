package io.github.redouanebali.security;

import java.util.Set;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@NoArgsConstructor
public final class SecurityUtil {

  // Return the stable user id we use as ownerId (e.g., the JWT "sub")
  public static String currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof final JwtAuthenticationToken jwtAuth)) {
      // Covers null, anonymous, or any non-JWT authentication
      return null;
    }
    return jwtAuth.getToken().getClaimAsString("email");
  }

  public static boolean isSuperAdmin(Set<String> superAdmins) {
    String me = currentUserId();
    return superAdmins.contains(me);
  }
}