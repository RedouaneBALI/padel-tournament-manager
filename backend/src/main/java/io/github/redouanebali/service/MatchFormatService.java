package io.github.redouanebali.service;

import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchFormatService {

  private final TournamentRepository tournamentRepository;

  public MatchFormat getMatchFormatForRound(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);
    return tournament.getRounds().stream()
                     .filter(round -> round.getStage() == stage)
                     .findFirst()
                     .map(Round::getMatchFormat)
                     .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
  }

  public MatchFormat updateMatchFormatForRound(Long tournamentId, Stage stage, MatchFormat newFormat) {
    Tournament tournament = getTournamentById(tournamentId);
    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));
    MatchFormat currentFormat = round.getMatchFormat();

    if (currentFormat == null) {
      round.setMatchFormat(newFormat);
    } else {
      currentFormat.setNumberOfSetsToWin(newFormat.getNumberOfSetsToWin());
      currentFormat.setPointsPerSet(newFormat.getPointsPerSet());
      currentFormat.setAdvantage(newFormat.isAdvantage());
      currentFormat.setSuperTieBreakInFinalSet(newFormat.isSuperTieBreakInFinalSet());
    }

    tournamentRepository.save(tournament);
    return newFormat;
  }

  private Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }
}
