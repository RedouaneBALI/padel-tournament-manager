package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class QualifyMainRoundGenerator extends AbstractRoundGenerator {

  private int mainDrawSize;
  private int nbQualifiers;
  // @todo add nbSeeds for qualification rounds too

  public QualifyMainRoundGenerator(int nbSeeds, int mainDrawSize, int nbQualifiers) {
    super(nbSeeds);
    this.mainDrawSize = mainDrawSize;
    this.nbQualifiers = nbQualifiers;
  }

  public QualifyMainRoundGenerator(final int nbSeeds) {
    super(nbSeeds);
  }

  @Override
  public Round generateManualRound(final List<PlayerPair> allPairs) {
    // 1. Validation des données d'entrée
    final int totalPairs = (allPairs != null) ? allPairs.size() : 0;
    if (totalPairs <= 0) {
      throw new IllegalArgumentException("allPairs must not be empty");
    }

    // 2. Tri des paires en fonction de leur seed (ou autre critère)
    List<PlayerPair> sortedPairs = new ArrayList<>(allPairs);
    sortedPairs.sort(Comparator.comparingInt(PlayerPair::getSeed)); // Trier par seed croissant

    int directMainDrawPairs = mainDrawSize - nbQualifiers; // Paires directement qualifiées pour le main draw
    // Copie matérialisée pour pouvoir insérer des BYE
    List<PlayerPair> qualificationPairs = new ArrayList<>(sortedPairs.subList(directMainDrawPairs, sortedPairs.size()));

    // Assure une taille puissance de 2 en ajoutant des BYE, comme dans KnockoutRoundGenerator
    int originalQualSize = qualificationPairs.size();
    addMissingByePairsToReachPowerOfTwo(qualificationPairs, originalQualSize);

    Round round = new Round();

    // Crée les matchs vides en fonction de la taille puissance de 2
    List<Game> games = createEmptyGames(qualificationPairs.size(), round.getMatchFormat());

    // Remplissage séquentiel A puis B (même logique que KO)
    int teamIndex = 0;
    for (Game game : games) {
      if (teamIndex < qualificationPairs.size()) {
        game.setTeamA(qualificationPairs.get(teamIndex++));
      }
      if (teamIndex < qualificationPairs.size()) {
        game.setTeamB(qualificationPairs.get(teamIndex++));
      }
    }

    // 7. Déterminer le stage pour Q1
    Stage stage = Stage.Q1;

    // 8. Création du Round avec les matchs
    round.setStage(stage);
    round.addGames(games);
    return round;
  }

  /**
   * Overload: Generates Q1 and pre-seeds main draw with direct entrants, leaving nbQualifiers null slots.
   */
  public Round generateManualRound(final Tournament tournament, final List<PlayerPair> allPairs) {
    if (tournament == null) {
      throw new IllegalArgumentException("tournament must not be null");
    }
    if (allPairs == null || allPairs.isEmpty()) {
      throw new IllegalArgumentException("allPairs must not be empty");
    }

    // Sort all pairs by ascending seed (best first)
    List<PlayerPair> sorted = new ArrayList<>(allPairs);
    sorted.sort(Comparator.comparingInt(PlayerPair::getSeed));

    // Read sizes from tournament config if not provided via constructor
    int
        cfgMainDrawSize =
        (this.mainDrawSize > 0) ? this.mainDrawSize : (tournament.getConfig() != null ? tournament.getConfig().getMainDrawSize() : 0);
    int
        cfgNbQualifiers =
        (this.nbQualifiers > 0) ? this.nbQualifiers : (tournament.getConfig() != null ? tournament.getConfig().getNbQualifiers() : 0);

    // Determine direct main-draw entrants and qualifiers pool
    int              directMainDrawPairs = Math.max(0, Math.min(cfgMainDrawSize - cfgNbQualifiers, sorted.size()));
    List<PlayerPair> directEntrants      = new ArrayList<>(sorted.subList(0, directMainDrawPairs));

    // Seed first main round with direct entrants (leave exactly nbQualifiers null slots)
    seedFirstMainRound(tournament, directEntrants);

    // Build and return Q1 using the remaining pairs (align with KO: reach power-of-two via BYE, then fill sequentially)
    List<PlayerPair> remaining = new ArrayList<>(sorted.subList(directMainDrawPairs, sorted.size()));

    int originalQualSize = remaining.size();
    addMissingByePairsToReachPowerOfTwo(remaining, originalQualSize);

    Round q1 = new Round();

    List<Game> games = createEmptyGames(remaining.size(), q1.getMatchFormat());

    int teamIndex = 0;
    for (Game game : games) {
      if (teamIndex < remaining.size()) {
        game.setTeamA(remaining.get(teamIndex++));
      }
      if (teamIndex < remaining.size()) {
        game.setTeamB(remaining.get(teamIndex++));
      }
    }

    q1.setStage(Stage.Q1);
    q1.addGames(games);
    return q1;
  }

  /**
   * Seeds the first main round with direct entrants, leaving nbQualifiers null slots for qualifiers.
   */
  private void seedFirstMainRound(final Tournament tournament, final List<PlayerPair> directEntrants) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().isEmpty()) {
      return;
    }
    Round firstFinalRound = null;
    for (Round r : tournament.getRounds()) {
      if (r.getStage() != null && !r.getStage().isQualification()) {
        firstFinalRound = r;
        break;
      }
    }
    if (firstFinalRound == null || firstFinalRound.getGames() == null) {
      return;
    }

    int idx = 0;
    for (Game g : firstFinalRound.getGames()) {
      if (idx < directEntrants.size()) {
        g.setTeamA(directEntrants.get(idx++));
      }
      if (idx < directEntrants.size()) {
        g.setTeamB(directEntrants.get(idx++));
      }
      // If we run out of direct entrants, we keep remaining slots null as placeholders for future qualifiers
    }
  }

  @Override
  public void propagateWinners(Tournament tournament) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }

    List<Round> rounds = tournament.getRounds();

    // Identifier les rounds de préqualification et de phase finale
    List<Round> qualifyingRounds = new ArrayList<>();
    List<Round> finalRounds      = new ArrayList<>();

    for (Round round : rounds) {
      if (round.getStage().isQualification()) {
        qualifyingRounds.add(round); // Les rounds Q1, Q2, Q3 ou similaires
      } else {
        finalRounds.add(round); // Les rounds finaux (R16, QF, etc.)
      }
    }

    // Étape 1 : Propager les vainqueurs dans les rounds de préqualification
    for (int roundIndex = 0; roundIndex < qualifyingRounds.size() - 1; roundIndex++) {
      Round currentRound = qualifyingRounds.get(roundIndex);
      Round nextRound    = qualifyingRounds.get(roundIndex + 1);
      propagateWinnersBetweenRounds(currentRound, nextRound);
    }

    // Étape 2 : Injecter les vainqueurs du dernier round de qualification dans la phase finale
    if (!qualifyingRounds.isEmpty() && !finalRounds.isEmpty()) {
      if (!qualifyingRounds.stream().allMatch(Round::isFinished)) {
        return;
      }
      Round lastQualifyingRound = qualifyingRounds.get(qualifyingRounds.size() - 1);
      Round firstFinalRound     = finalRounds.get(0);

      propagateWinnersBetweenRounds(lastQualifyingRound, firstFinalRound);
    }
  }

  /**
   * Propage les gagnants entre deux rounds.
   */
  private void propagateWinnersBetweenRounds(Round currentRound, Round nextRound) {
    if (currentRound == null || nextRound == null || currentRound.getGames() == null || nextRound.getGames() == null) {
      return;
    }

    List<Game> currentGames = currentRound.getGames();
    List<Game> nextGames    = nextRound.getGames();

    // Vérifie si le round actuel est vide
    boolean currentRoundEmpty = currentGames.stream()
                                            .allMatch(game -> game.getTeamA() == null && game.getTeamB() == null);
    if (currentRoundEmpty) {
      return;
    }

    for (int i = 0; i < currentGames.size(); i++) {
      Game       currentGame = currentGames.get(i);
      PlayerPair winner      = null;

      // Récupération de l'équipe gagnante (même logique que KO : BYE explicite ou match terminé)
      if (currentGame.getTeamA() != null && currentGame.getTeamA().isBye()) {
        winner = currentGame.getTeamB();
      } else if (currentGame.getTeamB() != null && currentGame.getTeamB().isBye()) {
        winner = currentGame.getTeamA();
      } else if (currentGame.isFinished()) {
        winner = currentGame.getWinner();
      }

      System.out.println("Processing Game: " + currentGame);
      System.out.println("Winner identified: " + (winner != null ? winner : "No winner yet!"));

      int targetGameIndex = i / 2;
      if (targetGameIndex >= nextGames.size()) {
        System.err.println("Target game index out of bounds for Q2: " + targetGameIndex);
        continue;
      }

      Game nextGame = nextGames.get(targetGameIndex);

      // Placement des gagnants dans le prochain round
      if (winner == null) {
        if (i % 2 == 0) {
          nextGame.setTeamA(null);
        } else {
          nextGame.setTeamB(null);
        }
      } else {
        if (i % 2 == 0) {
          nextGame.setTeamA(winner);
        } else {
          nextGame.setTeamB(winner);
        }
      }
    }
  }

  @Override
  public Round generateAlgorithmicRound(final List<PlayerPair> pairs) {
    // @todo plus tard
    throw new UnsupportedOperationException("Use generateManualRound(Tournament, pairs) for Qualify/Main format");
  }

  @Override
  public List<Round> createRoundsStructure(final Tournament tournament) {
    List<Round> rounds = new ArrayList<>();
    if (tournament.getConfig() == null) {
      return rounds;
    }

    // -------- Pré-qualif : Qk .. Q1 (VIDES, juste structure + games placeholders) --------
    int pre  = tournament.getConfig().getPreQualDrawSize();
    int qual = Math.max(1, tournament.getConfig().getNbQualifiers());
    if (pre > 0 && DrawMath.isPowerOfTwo(pre) && pre % qual == 0 && DrawMath.isPowerOfTwo(pre / qual)) {
      int ratio   = pre / qual;        // ex. 32/4 = 8 → Q1,Q2,Q3
      int qRounds = Math.min(3, log2Safe(ratio)); // au plus Q1,Q2,Q3
      for (int i = 1; i <= qRounds; i++) { // Q1 puis Q2 puis Q3
        Stage stage = Stage.valueOf("Q" + i);
        int   games = pre >> i; // Q1: pre/2, Q2: pre/4, Q3: pre/8
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
