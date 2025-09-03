package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class TournamentBuilder {

  private final List<TournamentPhase> phases = new ArrayList<>();

  public List<Round> buildQualifKOStructure(Tournament t) {
    var         cfg    = t.getConfig();
    List<Round> rounds = new ArrayList<>();
    phases.clear();

    if (cfg.getPreQualDrawSize() != null && cfg.getPreQualDrawSize() > 0 && cfg.getNbQualifiers() != null && cfg.getNbQualifiers() > 0) {
      TournamentPhase qualifs = new KnockoutPhase(
          cfg.getPreQualDrawSize(),
          cfg.getNbSeedsQualify(),
          PhaseType.QUALIFS,
          cfg.getDrawMode()
      );
      rounds.addAll(qualifs.initialize(t));
      phases.add(qualifs);
    }

    TournamentPhase mainDraw = new KnockoutPhase(
        cfg.getMainDrawSize(),
        cfg.getNbSeeds(),
        PhaseType.MAIN_DRAW,
        cfg.getDrawMode()
    );
    rounds.addAll(mainDraw.initialize(t));
    phases.add(mainDraw);

    return rounds;
  }

  /**
   * Propagate winners across all phases sequentially.
   */
  public void propagateWinners(Tournament t) {
    if (t == null || phases.isEmpty()) {
      return;
    }
    phases.getFirst().propagateWinners(t);
  }

  public List<String> validate(Tournament t) {
    List<String> errors = new ArrayList<>();
    if (t == null) {
      errors.add("Tournament is null");
      return errors;
    }

    var config = t.getConfig();
    if (config == null) {
      errors.add("Tournament configuration is null");
      return errors;
    }

    int mainDrawSize = config.getMainDrawSize();
    int nbSeeds      = config.getNbSeeds();

    // Validate mainDrawSize is a power of 2
    if (mainDrawSize <= 0 || (mainDrawSize & (mainDrawSize - 1)) != 0) {
      errors.add("mainDrawSize must be a power of 2, got: " + mainDrawSize);
    }

    // Validate nbSeeds doesn't exceed mainDrawSize
    if (nbSeeds > mainDrawSize) {
      errors.add("nbSeeds (" + nbSeeds + ") cannot exceed mainDrawSize (" + mainDrawSize + ")");
    }

    // Validate other phases if they exist
    if (!phases.isEmpty()) {
      for (TournamentPhase phase : phases) {
        errors.addAll(phase.validate(t));
      }
    }

    return errors;
  }

  /**
   * Effectue le tirage au sort et remplit les premiers rounds (Q1 et premier round finale).
   *
   * @param tournament le tournoi à remplir
   * @param allPairs la liste complète des équipes (triée par seed croissant)
   */
  public void drawLotsAndFillInitialRounds(Tournament tournament, List<PlayerPair> allPairs) {
    // Récupère la config
    var cfg             = tournament.getConfig();
    int preQualDrawSize = cfg.getPreQualDrawSize();
    int mainDrawSize    = cfg.getMainDrawSize();
    int nbSeeds         = cfg.getNbSeeds();

    // Trie les équipes par seed croissant (meilleurs d'abord)
    List<PlayerPair> sortedPairs = new ArrayList<>(allPairs);
    sortedPairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // Sépare les équipes pour la qualif et la phase finale
    List<PlayerPair> qualifPairs = new ArrayList<>();
    List<PlayerPair> finalePairs = new ArrayList<>();

    if (preQualDrawSize > 0) {
      // Les moins bien classés jouent la qualif
      qualifPairs.addAll(sortedPairs.subList(sortedPairs.size() - preQualDrawSize, sortedPairs.size()));
      // Les mieux classés jouent la phase finale
      finalePairs.addAll(sortedPairs.subList(0, sortedPairs.size() - preQualDrawSize));
    } else {
      // Pas de qualif, tout le monde en finale
      finalePairs.addAll(sortedPairs);
    }

    // Récupère les rounds
    List<Round> rounds = tournament.getRounds();

    // 1. Remplir Q1 si elle existe
    if (preQualDrawSize > 0 && !rounds.isEmpty()) {
      Round         q1          = rounds.get(0);
      KnockoutPhase qualifPhase = new KnockoutPhase(preQualDrawSize, 0, PhaseType.QUALIFS, DrawMode.SEEDED);
      qualifPhase.placeSeedTeams(q1, qualifPairs, 0); // pas de seed en qualif
      qualifPhase.placeByeTeams(q1, qualifPairs.size(), preQualDrawSize, 0);
      qualifPhase.placeRemainingTeamsRandomly(q1, qualifPairs);
    }

    // 2. Remplir le premier round de la phase finale
    // Cherche le round de la phase finale (après les qualifs)
    Round firstFinaleRound = null;
    for (Round r : rounds) {
      if (r.getStage().isMainDraw(mainDrawSize)) {
        firstFinaleRound = r;
        break;
      }
    }
    if (firstFinaleRound != null) {
      KnockoutPhase finalePhase = new KnockoutPhase(mainDrawSize, nbSeeds, PhaseType.MAIN_DRAW, DrawMode.SEEDED);
      finalePhase.placeSeedTeams(firstFinaleRound, finalePairs, nbSeeds);
      finalePhase.placeByeTeams(firstFinaleRound, finalePairs.size(), mainDrawSize, nbSeeds);
      finalePhase.placeRemainingTeamsRandomly(firstFinaleRound, finalePairs);
    }
  }

  /**
   * Remplit les premiers rounds (Q1 et premier round du tableau principal) en mode manuel. Les rounds et les équipes sont déjà définis par
   * l'utilisateur. Seuls les rounds Q1 et premier round du tableau principal sont remplacés, les autres restent inchangés.
   *
   * @param tournament le tournoi à remplir
   * @param initialRounds la liste des rounds à utiliser (Q1, puis premier round du tableau principal)
   */
  public void fillInitialRoundsManual(Tournament tournament, List<Round> initialRounds) {
    if (initialRounds == null || initialRounds.isEmpty()) {
      return;
    }
    List<Round> rounds = tournament.getRounds();
    if (rounds == null || rounds.isEmpty()) {
      return;
    }

    // On suppose initialRounds[0] = Q1, initialRounds[1] = premier round main draw
    int replaced = 0;
    for (int i = 0; i < rounds.size() && replaced < initialRounds.size(); i++) {
      Round r        = rounds.get(i);
      Round provided = initialRounds.get(replaced);

      // Remplace Q1 si le stage correspond
      if (replaced == 0 && r.getStage().name().equals(provided.getStage().name())) {
        rounds.set(i, provided);
        replaced++;
      }
      // Remplace le premier round du tableau principal
      else if (replaced == 1 && r.getStage().name().equals(provided.getStage().name())) {
        rounds.set(i, provided);
        replaced++;
      }
    }
  }
}
