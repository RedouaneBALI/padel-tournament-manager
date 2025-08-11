package io.github.redouanebali.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // API stateless => pas de CSRF
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin/**", "/api/admin/**").authenticated()
            .anyRequest().permitAll()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs"))
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      // Liste CSV des origins autorisées (modifiable via application.properties)
      @Value("${app.cors.allowed-origins:https://nextapp-tcepdy5iwa-uc.a.run.app,http://localhost:3000,192.168.1.9:3000}")
      String originsCsv
  ) {
    List<String> origins = Arrays.stream(originsCsv.split(","))
                                 .map(String::trim)
                                 .filter(s -> !s.isBlank())
                                 .toList();

    CorsConfiguration config = new CorsConfiguration();
    // Avec allowCredentials(true), on ne peut pas utiliser "*" -> on passe par patterns
    config.setAllowedOriginPatterns(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(Duration.ofHours(1)); // cache du préflight

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // Appliquer à toutes les routes. Si tu préfères, remplace par "/api/**".
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder
        .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
        .build();
  }
}
