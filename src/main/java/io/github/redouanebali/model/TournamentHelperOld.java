package io.github.redouanebali.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TournamentHelperOld {

  public static List<Round> affectPlayerPairsToRounds(List<PlayerPair> pairs) {
    List<Round>      roundList   = new ArrayList<>();
    List<PlayerPair> sortedPairs = new ArrayList<>(pairs);
    sortedPairs.sort(Comparator.comparingInt(PlayerPair::getSeed)); // Lower seed = better team

    int totalTeams = sortedPairs.size();

    int mainDrawSize = 1;
    while (mainDrawSize * 2 <= totalTeams) {
      mainDrawSize *= 2;
    }

    int preliminaryTeamsCount = totalTeams - mainDrawSize;
    int preliminaryRoundTeams = preliminaryTeamsCount * 2;

    // Identify teams for preliminary (worst teams)
    List<PlayerPair> preliminaryTeams       = new ArrayList<>();
    List<PlayerPair> directlyQualifiedTeams = new ArrayList<>();

    if (preliminaryRoundTeams > 0) {
      // Worst teams play preliminary round (R64, R128, etc.)
      preliminaryTeams = new ArrayList<>(sortedPairs.subList(totalTeams - preliminaryRoundTeams, totalTeams));
      // Best teams are directly qualified
      directlyQualifiedTeams = new ArrayList<>(sortedPairs.subList(0, mainDrawSize - preliminaryTeamsCount));
    } else {
      // All teams directly qualify (no preliminary round needed)
      directlyQualifiedTeams = new ArrayList<>(sortedPairs);
    }

    // Build round list
    List<RoundInfo> roundInfos = getRounds(mainDrawSize);

    // Add preliminary round if needed
    if (preliminaryRoundTeams > 0) {
      RoundInfo qualifyingRoundInfo = RoundInfo.fromNbTeams(mainDrawSize * 2);
      Round     qualifyingRound     = new Round(qualifyingRoundInfo);
      qualifyingRound.setPlayerPairs(preliminaryTeams);
      roundList.add(qualifyingRound);

      // Add main draw round (e.g. R32)
      RoundInfo mainRoundInfo = RoundInfo.fromNbTeams(mainDrawSize);
      Round     mainRound     = new Round(mainRoundInfo);
      mainRound.setPlayerPairs(directlyQualifiedTeams);
      roundList.add(mainRound);

      // Add subsequent rounds (empty teams)
      RoundInfo nextInfo = mainRoundInfo.next();
      while (nextInfo != null) {
        roundList.add(new Round(nextInfo));
        nextInfo = nextInfo.next();
      }
    } else {
      // No preliminary round, only main draw and subsequent rounds
      for (RoundInfo roundInfo : roundInfos) {
        Round round = new Round(roundInfo);
        if (roundInfo == RoundInfo.fromNbTeams(mainDrawSize)) {
          round.setPlayerPairs(directlyQualifiedTeams);
        }
        roundList.add(round);
      }
    }

    return roundList;
  }

  private static List<RoundInfo> getRounds(int mainDrawSize) {
    List<RoundInfo> roundInfos = new ArrayList<>();
    RoundInfo       info       = RoundInfo.fromNbTeams(mainDrawSize);
    while (info != null) {
      roundInfos.add(info);
      info = info.next();
    }
    return roundInfos;
  }
}