package io.github.redouanebali.controller;

import io.github.redouanebali.dto.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.PoolRanking;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.GroupRankingService;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class PublicTournamentController {

  @Autowired
  private TournamentService tournamentService;

  @Autowired
  private PlayerPairService playerPairService;

  @Autowired
  private GroupRankingService groupRankingService;

  @Autowired
  private MatchFormatService matchFormatService;

  @Autowired
  private TournamentMapper tournamentMapper;

  @GetMapping("/{id}")
  public TournamentDTO getTournament(@PathVariable Long id) {
    Tournament tournament = tournamentService.getTournamentById(id);
    return tournamentMapper.toDTO(tournament);
  }

  @GetMapping("/{id}/pairs")
  public List<PlayerPair> getPairs(@PathVariable Long id) {
    return playerPairService.getPairsByTournamentId(id);
  }

  @GetMapping("/{id}/rounds")
  public List<Round> getRounds(@PathVariable Long id) {
    Tournament tournament = tournamentService.getTournamentById(id);
    return tournament.getRounds().stream()
                     .sorted(Comparator.comparing(r -> r.getStage().getOrder()))
                     .collect(Collectors.toList());
  }

  @GetMapping("/{id}/rounds/{stage}/games")
  public Set<Game> getGamesByStage(@PathVariable Long id, @PathVariable Stage stage) {
    return tournamentService.getGamesByTournamentAndStage(id, stage);
  }

  @GetMapping("/{id}/rounds/{stage}/match-format")
  public MatchFormat getMatchFormat(@PathVariable Long id, @PathVariable Stage stage) {
    return matchFormatService.getMatchFormatForRound(id, stage);
  }


  @GetMapping("/{id}/groups/ranking")
  public List<PoolRanking> getGroupRankings(@PathVariable Long id) {
    return GroupRankingService.getGroupRankings(tournamentService.getTournamentById(id));
  }
}
