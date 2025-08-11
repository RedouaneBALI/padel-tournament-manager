package io.github.redouanebali.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProps {

  // map env SUPER_ADMINS via spring.config.import / or @Value
  @Value("${SUPER_ADMINS:}")
  private String superAdminsRaw;

  public Set<String> getSuperAdmins() {
    if (superAdminsRaw == null || superAdminsRaw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(superAdminsRaw.split(","))
                 .map(String::trim)
                 .filter(s -> !s.isEmpty())
                 .collect(Collectors.toSet());
  }
}