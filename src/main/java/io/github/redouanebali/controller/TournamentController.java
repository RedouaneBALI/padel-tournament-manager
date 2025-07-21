package io.github.redouanebali.controller;

import io.github.redouanebali.dto.RoundDTO;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
public class TournamentController {

  @Autowired
  private TournamentService tournamentService;

  @PostMapping
  public Tournament createTournament(@RequestBody Tournament tournament) {
    System.out.println("createTournament");
    return tournamentService.createTournament(tournament);
  }

  @PostMapping("/{id}/draw")
  public RoundDTO generateDraw(@PathVariable("id") Long tournamentId) {
    System.out.println("generateDraw");
    return new RoundDTO(tournamentService.generateDraw(tournamentId));
  }
}
