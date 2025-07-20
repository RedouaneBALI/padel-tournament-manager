package io.github.redouanebali.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TournamentHelper {

  /**
   * Génère le premier round avec toutes les équipes, complétées par des BYES si besoin.
   *
   * @param pairs La liste des équipes (PlayerPair)
   * @return La liste du premier round, chaque entrée étant un PlayerPair (équipe ou BYE)
   */
  public static List<PlayerPair> generateFirstRoundWithByes(List<PlayerPair> pairs) {
    // Trie les équipes si tu veux garantir un ordre particulier (ex : par seed)
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));

    int totalTeams = pairs.size();

    // Trouve la prochaine puissance de 2 supérieure ou égale
    int drawSize = 1;
    while (drawSize < totalTeams) {
      drawSize *= 2;
    }

    List<PlayerPair> roundPairs = new ArrayList<>(pairs);

    // Ajoute les BYES si besoin
    int byesToAdd = drawSize - totalTeams;
    for (int i = 0; i < byesToAdd; i++) {
      // Tu peux créer un PlayerPair spécial pour le BYE
      Player     byePlayer = new Player(-1L, "BYE", 0, 0, 0);
      PlayerPair byePair   = new PlayerPair(byePlayer, byePlayer, 0);
      roundPairs.add(byePair);
    }

    return roundPairs;
  }

  /**
   * Génère la liste des matchs du premier tour selon l'ordre "snake".
   *
   * @param pairs La liste des équipes (PlayerPair), triée par seed croissant.
   * @return Liste de Game (matchs du premier tour).
   */
  public static List<Game> generateFirstRoundGamesSnake(List<PlayerPair> pairs) {
    pairs.sort(Comparator.comparingInt(PlayerPair::getSeed));
    List<PlayerPair> orderedPairs = snakeOrder(pairs);

    List<Game> games = new ArrayList<>();
    int        n     = orderedPairs.size();
    for (int i = 0; i < n / 2; i++) {
      PlayerPair teamA = orderedPairs.get(i);
      PlayerPair teamB = orderedPairs.get(n - 1 - i);

      // N'ajoute le match que s'il y a AU MOINS UNE équipe réelle
      if (!isBye(teamA) || !isBye(teamB)) {
        games.add(new Game(teamA, teamB));
      }
      // Si les deux sont BYE, on ignore ce match
    }
    return games;
  }

  private static boolean isBye(PlayerPair pair) {
    return pair.getPlayer1().getId() == -1L && pair.getPlayer2().getId() == -1L;
  }

  /**
   * Placement "snake" des équipes dans le tableau.
   *
   * @param pairs Liste triée par seed croissant.
   * @return Liste ordonnée selon le placement snake.
   */
  public static List<PlayerPair> snakeOrder(List<PlayerPair> pairs) {
    int              n       = pairs.size();
    List<PlayerPair> ordered = new ArrayList<>(n);
    // Initialise la liste avec nulls
    for (int i = 0; i < n; i++) {
      ordered.add(null);
    }

    int     left    = 0;
    int     right   = n - 1;
    boolean toRight = true;
    for (PlayerPair pair : pairs) {
      if (toRight) {
        ordered.set(left, pair);
        left++;
      } else {
        ordered.set(right, pair);
        right--;
      }
      toRight = !toRight;
    }
    return ordered;
  }
}