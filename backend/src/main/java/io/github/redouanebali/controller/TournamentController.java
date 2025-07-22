package io.github.redouanebali.controller;

import io.github.redouanebali.dto.RoundDTO;
import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tournaments")
public class TournamentController {

  @Autowired
  private TournamentService tournamentService;

  @PostMapping
  public Tournament createTournament(@RequestBody Tournament tournament) {
    return tournamentService.createTournament(tournament);
  }

  @PostMapping("/{id}/draw")
  public RoundDTO generateDraw(@PathVariable("id") Long tournamentId) {
    return new RoundDTO(tournamentService.generateDraw(tournamentId));
  }

  @PostMapping("/{id}/pairs")
  public int addPairsToTournament(@PathVariable("id") Long tournamentId, @RequestBody List<SimplePlayerPairDTO> playerPairs) {
    return tournamentService.addPairs(tournamentId, playerPairs);
  }

  @GetMapping("/{id}/pairs")
  public List<SimplePlayerPairDTO> getPairsByTournament(@PathVariable Long id) {
    Tournament tournament = tournamentService.getTournamentById(id);
    return tournament.getPlayerPairs().stream()
                     .map(SimplePlayerPairDTO::fromPlayerPair)
                     .toList();
  }

}
