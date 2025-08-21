package io.github.redouanebali.generation;

import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.TournamentFormatConfig;
import java.util.ArrayList;
import java.util.List;

public class QualifyMainRoundGenerator extends AbstractRoundGenerator {

  public QualifyMainRoundGenerator(final int nbSeeds) {
    super(nbSeeds);
  }

  public Round generateManualRound(final Tournament tournament, final List<PlayerPair> pairs) {
    TournamentFormatConfig config = tournament.getConfig();
    if (config == null) {
      throw new IllegalArgumentException("Tournament formatConfig must be QualifMainConfig");
    }

    final int size = pairs != null ? pairs.size() : 0;
    if (size <= 0) {
      throw new IllegalArgumentException("pairs must not be empty");
    }

    // 1) Pré-qualif (au plus 2 tours): on déduit le numéro de Q depuis la taille courante.
    int pre  = config.getPreQualDrawSize();
    int qual = Math.max(1, config.getNbQualifiers());
    if (pre > 0 && DrawMath.isPowerOfTwo(pre) && size <= pre && size >= qual && (pre % size) == 0) {
      int qNum = 1 + log2Safe(pre / size); // size==pre -> Q1, size==pre/2 -> Q2
      if (qNum > 2) {
        qNum = 2;               // on borne à Q2 max
      }
      Stage stage = Stage.valueOf("Q" + qNum);
      return createManualRound(stage, pairs);
    }

    // 2) Cas Main Draw : on reçoit mainDrawSize (ou rempli avec BYE jusqu'à nextPow2)
    int main      = config.getMainDrawSize();
    int stageSize = DrawMath.isPowerOfTwo(size) ? size : DrawMath.nextPowerOfTwo(size);
    // Par convention, on cible le stage correspondant à la puissance de 2 couvrante
    if (size == main || stageSize == main) {
      Stage stage = Stage.fromNbTeams(stageSize);
      return createManualRound(stage, pairs);
    }

    // fallback : considérer comme un KO simple basé sur la taille reçue
    Stage stage = Stage.fromNbTeams(stageSize);
    return createManualRound(stage, pairs);
  }

  @Override
  public void propagateWinners(final Tournament tournament) {
    // On s'appuie sur la logique éprouvée du KO pour propager les vainqueurs entre tours successifs
    new KnockoutRoundGenerator(tournament.getConfig().getNbSeeds()).propagateWinners(tournament);
  }

  @Override
  public Round generateAlgorithmicRound(final List<PlayerPair> pairs) {
    // @todo plus tard
    throw new UnsupportedOperationException("Use generateManualRound(Tournament, pairs) for Qualify/Main format");
  }

  @Override
  public Round generateManualRound(final List<PlayerPair> pairs) {
    // Méthode héritée (sans tournoi) — on se rabat sur une déduction par taille
    if (pairs == null || pairs.isEmpty()) {
      throw new IllegalArgumentException("pairs must not be empty");
    }
    int size = pairs.size();
    // Sans accès à la config du tournoi, on assume qu'un appel manuel ici concerne la pré-qualif → Q1
    Stage stage = Stage.Q1;
    return createManualRound(stage, pairs);
  }

  @Override
  public List<Round> initRoundsAndGames(final Tournament tournament) {
    List<Round> rounds = new ArrayList<>();
    if (tournament.getConfig() == null) {
      return rounds;
    }

    // -------- Pré-qualif : Qk .. Q1 (VIDES, juste structure + games placeholders) --------
    int pre  = tournament.getConfig().getPreQualDrawSize();
    int qual = Math.max(1, tournament.getConfig().getNbQualifiers());
    if (pre > 0 && DrawMath.isPowerOfTwo(pre) && pre % qual == 0 && DrawMath.isPowerOfTwo(pre / qual)) {
      int ratio   = pre / qual;        // ex. 32/8 = 4
      int qRounds = Math.min(2, log2Safe(ratio)); // au plus Q1,Q2
      for (int i = 1; i <= qRounds; i++) { // Q1 puis Q2
        Stage stage = Stage.valueOf("Q" + i);
        int   games = pre >> i; // Q1: pre/2, Q2: pre/4
        rounds.add(createEmptyRound(stage, games));
      }
    }

// -------- Main draw : construire TOUT le bracket (Rxx → QUARTERS → SEMI → FINAL) --------
    int main = tournament.getConfig().getMainDrawSize();
    if (main > 0) {
      if (!DrawMath.isPowerOfTwo(main)) {
        // Par sécurité : arrondir à la puissance de 2 supérieure
        main = DrawMath.nextPowerOfTwo(main);
      }
      int teams = main;
      while (teams >= 2) {
        Stage stage = Stage.fromNbTeams(teams);
        rounds.add(createEmptyRound(stage, teams / 2));
        teams >>= 1; // divise par 2 à chaque tour
      }
    }

    return rounds;
  }

}
