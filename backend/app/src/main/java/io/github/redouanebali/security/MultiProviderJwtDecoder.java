package io.github.redouanebali.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;

@Slf4j
public class MultiProviderJwtDecoder implements JwtDecoder {

  private final JwtDecoder   googleDecoder;
  private final RestClient   restClient;
  private final ObjectMapper objectMapper;

  public MultiProviderJwtDecoder() {
    this.googleDecoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    this.objectMapper  = new ObjectMapper();

    // On construit le client HTTP qui ignore Zscaler
    this.restClient = RestClient.builder()
                                .requestFactory(createUnsafeRequestFactory())
                                .build();
  }

  /**
   * Crée une factory Apache HttpClient 5 configurée pour tout accepter (TrustAll). C'est la méthode la plus fiable sous Spring Boot 3.
   */
  private HttpComponentsClientHttpRequestFactory createUnsafeRequestFactory() {
    try {
      // 1. On crée un contexte SSL qui fait confiance à tout le monde (TrustAllStrategy)
      final SSLContext sslContext = SSLContextBuilder.create()
                                                     .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                                                     .build();

      // 2. On configure la factory de socket pour utiliser ce contexte et ne pas vérifier les noms d'hôtes
      final var sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                                                                    .setSslContext(sslContext)
                                                                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                                                    .build();

      // 3. On l'injecte dans le gestionnaire de connexions
      final var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                                                                             .setSSLSocketFactory(sslSocketFactory)
                                                                             .build();

      // 4. On crée le client final
      CloseableHttpClient httpClient = HttpClients.custom()
                                                  .setConnectionManager(connectionManager)
                                                  .build();

      return new HttpComponentsClientHttpRequestFactory(httpClient);

    } catch (Exception e) {
      // Si ça échoue ici, c'est critique, on ne veut pas continuer avec un client par défaut
      log.error("FATAL: Impossible de créer le client HTTP unsafe pour Zscaler", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Jwt decode(String token) throws JwtException {
    // Google tokens start with specific header chars usually, but this logic relies on structure
    // Simple check: Google tokens are strict JWTs, Facebook are opaque strings often
    if (token.startsWith("eyJ")) {
      try {
        return googleDecoder.decode(token);
      } catch (JwtException e) {
        // Si ça échoue avec Google, on tente Facebook (cas où le token commence aussi par eyJ mais est invalide google)
        log.warn("Token JWT non reconnu par Google, tentative Facebook...");
        return decodeFacebookToken(token);
      }
    } else {
      return decodeFacebookToken(token);
    }
  }

  private Jwt decodeFacebookToken(String token) {
    try {
      // L'appel partira avec le client "Unsafe" configuré plus haut
      String response = restClient.get()
                                  .uri("https://graph.facebook.com/me?fields=id,email,name&access_token=" + token)
                                  .retrieve()
                                  .body(String.class);

      JsonNode json = objectMapper.readTree(response);

      // Gestion des erreurs renvoyées par Facebook dans le JSON (cas rare où le statut est 200 mais body contient error)
      if (json.has("error")) {
        throw new RuntimeException("Facebook API Error: " + json.get("error").toPrettyString());
      }

      String id    = json.get("id").asText();
      String email = json.has("email") ? json.get("email").asText() : id + "@facebook.com";
      String name  = json.has("name") ? json.get("name").asText() : "Facebook User";

      Map<String, Object> claims = Map.of(
          "sub", id,
          "email", email,
          "name", name,
          "iss", "facebook",
          "iat", Instant.now().getEpochSecond(),
          "exp", Instant.now().plusSeconds(3600).getEpochSecond()
      );

      return new Jwt(token, Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"), claims);

    } catch (Exception e) {
      log.error("Erreur validation token Facebook: {}", e.getMessage());
      // Important : lancer une JwtException pour que Spring Security comprenne que l'auth a échoué
      throw new JwtException("Impossible de valider le token Facebook", e);
    }
  }
}