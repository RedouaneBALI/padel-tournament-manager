package io.github.redouanebali.config;

import io.github.redouanebali.service.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !h2")
public class CacheWarmup {

  private static final Logger log = LoggerFactory.getLogger(CacheWarmup.class);

  private final TournamentService tournamentService;

  public CacheWarmup(TournamentService tournamentService) {
    this.tournamentService = tournamentService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    // Run in a separate thread to avoid blocking startup
    new Thread(() -> {
      try {
        log.info("Cache warmup: loading activeTournaments into cache");
        tournamentService.getActiveTournaments();
        log.info("Cache warmup: done");
      } catch (Exception e) {
        log.warn("Cache warmup failed", e);
      }
    }, "cache-warmup").start();
  }
}
