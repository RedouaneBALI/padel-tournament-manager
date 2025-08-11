package io.github.redouanebali.config;

import java.util.Set;
import lombok.NoArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@NoArgsConstructor
public final class SecurityUtil {

  // Return the stable user id we use as ownerId (e.g., the JWT "sub")
  public static String currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new AccessDeniedException("No authenticated user");
    }
    if (!(auth instanceof final JwtAuthenticationToken jwtAuth)) {
      throw new AccessDeniedException("Email is not available for non-JWT authentication");
    }
    return jwtAuth.getToken().getClaimAsString("email");
  }

  public static boolean isSuperAdmin(Set<String> superAdmins) {
    String me = currentUserId();
    return superAdmins.contains(me);
  }
}