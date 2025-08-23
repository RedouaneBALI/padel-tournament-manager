package io.github.redouanebali.generation;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.format.DrawMath;
import io.github.redouanebali.model.format.TournamentFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class KnockoutRoundGenerator extends AbstractRoundGenerator {

  public KnockoutRoundGenerator(final int nbSeeds) {
    super(nbSeeds);
  }

  @Override
  public Round generateAlgorithmicRound(List<PlayerPair> pairs) {
    int originalSize = pairs.size();
    addMissingByePairsToReachPowerOfTwo(pairs, originalSize);
    List<Game>       games     = createEmptyGames(pairs.size());
    List<PlayerPair> remaining = placeSeedAndByeTeams(games, pairs, getNbSeeds());
    placeRemainingTeamsRandomly(games, remaining, getNbSeeds());
    Round round = new Round();
    round.addGames(games);
    round.setStage(Stage.fromNbTeams(pairs.size()));
    return round;
  }

  @Override
  public Round generateManualRound(final List<PlayerPair> pairs) {
    int originalSize = pairs.size();
    addMissingByePairsToReachPowerOfTwo(pairs, originalSize);
    List<Game> games = createEmptyGames(pairs.size());

    int teamIndex = 0;
    for (Game game : games) {
      if (teamIndex < pairs.size()) {
        game.setTeamA(pairs.get(teamIndex++));
      }
      if (teamIndex < pairs.size()) {
        game.setTeamB(pairs.get(teamIndex++));
      }
    }

    Round round = new Round();
    round.addGames(games);
    round.setStage(Stage.fromNbTeams(pairs.size()));
    return round;
  }


  /**
   * Place seeds and bye teams at the stratégic positions
   */
  private List<PlayerPair> placeSeedAndByeTeams(List<Game> games, List<PlayerPair> pairs, int nbSeeds) {
    List<PlayerPair> seeds = pairs.stream()
                                  .filter(p -> !p.isBye())
                                  .sorted(Comparator.comparingInt(PlayerPair::getSeed))
                                  .limit(nbSeeds)
                                  .toList();

    List<PlayerPair> byeTeams = pairs.stream()
                                     .filter(PlayerPair::isBye)
                                     .toList();

    List<Integer> seedsPositions = getSeedsPositions(pairs.size(), nbSeeds);

    for (int i = 0; i < seeds.size(); i++) {
      int gameIndex = seedsPositions.get(i) / 2;
      games.get(gameIndex).setTeamA(seeds.get(i));

      if (i < byeTeams.size()) {
        games.get(gameIndex).setTeamB(byeTeams.get(i));
      }
    }

    return new ArrayList<>(
        pairs.stream()
             .filter(p -> !seeds.contains(p) && !byeTeams.contains(p))
             .toList()
    );
  }

  public void propagateWinners(Tournament tournament) {
    if (tournament == null || tournament.getRounds() == null || tournament.getRounds().size() < 2) {
      return;
    }

    List<Round> rounds = tournament.getRounds();

    // Skip GROUPS; start from the first bracket round
    int startIndex = 0;
    while (startIndex < rounds.size() && rounds.get(startIndex).getStage() == Stage.GROUPS) {
      startIndex++;
    }

    for (int roundIndex = startIndex; roundIndex < rounds.size() - 1; roundIndex++) {
      Round currentRound = rounds.get(roundIndex);
      Round nextRound    = rounds.get(roundIndex + 1);

      // Skip propagation if currentRound is entirely empty (all games have null teamA and null teamB)
      boolean currentRoundEmpty = currentRound.getGames()
                                              .stream()
                                              .allMatch(g -> g.getTeamA() == null && g.getTeamB() == null);
      if (currentRoundEmpty) {
        continue;
      }

      // Safety: skip any non-bracket rounds just in case
      if (currentRound.getStage() == Stage.GROUPS || nextRound.getStage() == Stage.GROUPS) {
        continue;
      }

      List<Game> currentGames = currentRound.getGames();
      List<Game> nextGames    = nextRound.getGames();
      if (nextGames == null) {
        continue;
      }

      for (int i = 0; i < currentGames.size(); i++) {
        Game       currentGame = currentGames.get(i);
        PlayerPair winner      = null;

        if (currentGame.getTeamA() != null && currentGame.getTeamA().isBye()) {
          winner = currentGame.getTeamB();
        } else if (currentGame.getTeamB() != null && currentGame.getTeamB().isBye()) {
          winner = currentGame.getTeamA();
        } else if (currentGame.isFinished()) {
          winner = currentGame.getWinner();
        }

        int targetGameIndex = i / 2;
        if (targetGameIndex >= nextGames.size()) {
          // next round not large enough yet (shouldn't happen if rounds were initialized), skip safely
          continue;
        }
        Game nextGame = nextGames.get(targetGameIndex);

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
  }


  /**
   * Place aléatoirement les équipes non-seeds dans les positions libres
   */
  private void placeRemainingTeamsRandomly(List<Game> games, List<PlayerPair> remainingTeams, int nbSeeds) {
    Collections.shuffle(remainingTeams);

    int teamIndex = 0;
    for (Game game : games) {
      if (teamIndex >= remainingTeams.size()) {
        break;
      }

      if (game.getTeamA() != null && game.getTeamB() == null) {
        game.setTeamB(remainingTeams.get(teamIndex++));
      } else if (game.getTeamA() == null) {
        game.setTeamA(remainingTeams.get(teamIndex++));
        if (teamIndex < remainingTeams.size()) {
          game.setTeamB(remainingTeams.get(teamIndex++));
        }
      }
    }
  }

  /**
   * Compute recursively the positions of the seeds in a tournament
   *
   * @param nbTeams total number of teams
   * @param nbSeeds number of seeds teams
   * @return list of the positions of the seeds
   */
  public List<Integer> getSeedsPositions(int nbTeams, int nbSeeds) {
    List<Integer> allPositions = generateAllSeedPositions(nbTeams);
    return allPositions.subList(0, Math.min(nbSeeds, allPositions.size()));
  }

  /**
   * Generate all the possible position recursively from the bracket structure
   */
  private List<Integer> generateAllSeedPositions(int nbTeams) {
    if (nbTeams <= 1) {
      return Collections.singletonList(0);
    }

    int powerOfTwo = 1;
    while (powerOfTwo < nbTeams) {
      powerOfTwo *= 2;
    }

    List<Integer> fullPositions = generatePerfectSeedPositions(powerOfTwo);

    return fullPositions.subList(0, nbTeams);
  }

  /**
   * Génère les positions des seeds pour un bracket parfait (nbTeams doit être une puissance de 2)
   */
  private List<Integer> generatePerfectSeedPositions(int nbTeams) {
    if (nbTeams == 1) {
      return Collections.singletonList(0);
    }

    List<Integer> prev   = generatePerfectSeedPositions(nbTeams / 2);
    List<Integer> result = new ArrayList<>();

    for (int i = 0; i < prev.size(); i++) {
      int pos = prev.get(i);
      if (i % 2 == 0) {
        // Pour les indices pairs, on met la position en première moitié
        result.add(pos);
        result.add(nbTeams - 1 - pos);
      } else {
        // Pour les indices impairs, on inverse l'ordre
        result.add(nbTeams - 1 - pos);
        result.add(pos);
      }
    }

    return result;
  }

  @Override
  public List<Round> createRoundsStructure(Tournament tournament) {
    // Récupération du nombre maximal d'équipes
    int nbMaxTeams = tournament.getConfig().getNbMaxPairs(TournamentFormat.KNOCKOUT);

    // Validation : Vérifie si `nbMaxTeams` est une puissance de deux
    if (!DrawMath.isPowerOfTwo(nbMaxTeams)) {
      throw new IllegalArgumentException("Taille de tableau non supportée : " + nbMaxTeams);
    }

    // Création des rounds si le tableau est valide
    LinkedList<Round> rounds  = new LinkedList<>();
    Stage             current = Stage.fromNbTeams(nbMaxTeams);

    while (current != null && current != Stage.WINNER) {
      Round round = new Round(current);

      MatchFormat matchFormat = round.getMatchFormat();
      if (matchFormat != null && matchFormat.getId() == null) {
        round.setMatchFormat(matchFormat);
      }

      int nbMatches = current.getNbTeams() / 2;

      List<Game> games = new ArrayList<>();
      for (int i = 0; i < nbMatches; i++) {
        Game game = new Game(matchFormat); // Initialise un match (possiblement inutile, d'après votre commentaire)
        games.add(game);
      }

      round.addGames(games);
      rounds.add(round);

      current = current.next();
    }

    return rounds;
  }


}
