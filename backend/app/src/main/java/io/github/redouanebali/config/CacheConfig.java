package io.github.redouanebali.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test & !h2")
public class CacheConfig {

  @Value("${app.cache.activeTournaments.ttlMinutes:30}")
  private int activeTournamentsTtlMinutes;

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("activeTournaments");
    cacheManager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(activeTournamentsTtlMinutes, TimeUnit.MINUTES));
    return cacheManager;
  }
}
