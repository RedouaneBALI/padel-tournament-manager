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

    // 3. Identifier les paires jouant les qualifications
    int              directMainDrawPairs = mainDrawSize - nbQualifiers; // Paires directement qualifiées pour le main draw
    List<PlayerPair> qualificationPairs  = sortedPairs.subList(directMainDrawPairs, sortedPairs.size()); // Dernières paires

    // 4. Ajouter des BYEs pour atteindre la puissance de 2
    // Les BYEs sont ajoutés selon le besoin
    int powerOfTwoSize = 1;
    while (powerOfTwoSize < qualificationPairs.size()) {
      powerOfTwoSize *= 2;
    }
    int              numByes = powerOfTwoSize - qualificationPairs.size();
    List<PlayerPair> byes    = new ArrayList<>();
    for (int i = 0; i < numByes; i++) {
      byes.add(PlayerPair.bye());
    }

    // 5. Assigner les BYEs aux meilleures paires
    List<PlayerPair> fullRoundPairs = new ArrayList<>(qualificationPairs);
    for (int i = 0; i < numByes; i++) {
      fullRoundPairs.add(i * 2, byes.get(i)); // Intercaler les BYEs avec les meilleures paires
    }

    // 6. Créer les matchs (pairer les paires sans BYE vs BYE en priorité)
    List<Game> games     = new ArrayList<>();
    int        teamIndex = 0;
    while (teamIndex < fullRoundPairs.size()) {
      PlayerPair teamA = fullRoundPairs.get(teamIndex++);
      PlayerPair teamB = (teamIndex < fullRoundPairs.size()) ? fullRoundPairs.get(teamIndex++) : null;

      if (teamA.isBye() && teamB != null && teamB.isBye()) {
        throw new IllegalStateException("Match between BYE vs BYE is not allowed");
      }

      Game game = new Game();
      game.setTeamA(teamA);
      game.setTeamB(teamB);
      games.add(game);
    }

    // 7. Déterminer le stage pour Q1
    Stage stage = Stage.Q1;

    // 8. Création du Round avec les matchs
    Round round = new Round();
    round.setStage(stage);
    round.addGames(games);
    return round;
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

      // Récupération de l'équipe gagnante
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
