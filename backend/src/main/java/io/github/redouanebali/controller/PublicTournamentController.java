package io.github.redouanebali.controller;

import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.mapper.TournamentMapper;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.GroupRankingService;
import io.github.redouanebali.service.MatchFormatService;
import io.github.redouanebali.service.PlayerPairService;
import io.github.redouanebali.service.TournamentService;
import jakarta.annotation.security.PermitAll;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class PublicTournamentController {

  private final TournamentService tournamentService;

  private final PlayerPairService playerPairService;

  private final GroupRankingService groupRankingService;

  private final MatchFormatService matchFormatService;

  private final TournamentMapper tournamentMapper;

  @GetMapping("/{id}")
  public TournamentDTO getTournament(@PathVariable Long id) {
    Tournament tournament = tournamentService.getTournamentById(id);
    return tournamentMapper.toDTO(tournament);
  }

  @PermitAll
  @GetMapping("/{id}/pairs")
  public List<PlayerPairDTO> getPairs(@PathVariable Long id, boolean includeByes) {
    return tournamentMapper.toDTOPlayerPairList(playerPairService.getPairsByTournamentId(id, includeByes));
  }

  @GetMapping("/{id}/rounds")
  public List<RoundDTO> getRounds(@PathVariable Long id) {
    Tournament tournament = tournamentService.getTournamentById(id);
    return tournamentMapper.toDTORoundList(
        tournament.getRounds().stream()
                  .sorted(Comparator.comparing(r -> r.getStage().getOrder()))
                  .collect(Collectors.toList())
    );
  }

  @GetMapping("/{id}/rounds/{stage}/games")
  public Set<GameDTO> getGamesByStage(@PathVariable Long id, @PathVariable Stage stage) {
    return tournamentMapper.toDTOGameSet(tournamentService.getGamesByTournamentAndStage(id, stage));
  }

  @GetMapping("/{id}/rounds/{stage}/match-format")
  public MatchFormatDTO getMatchFormat(@PathVariable Long id, @PathVariable Stage stage) {
    return tournamentMapper.toDTO(matchFormatService.getMatchFormatForRound(id, stage));
  }


  @GetMapping("/{id}/groups/ranking")
  public List<PoolRankingDTO> getGroupRankings(@PathVariable Long id) {
    return tournamentMapper.toDTOPoolRankingList(
        GroupRankingService.getGroupRankings(tournamentService.getTournamentById(id))
    );
  }
}
