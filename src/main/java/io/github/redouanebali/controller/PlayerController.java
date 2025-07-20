package io.github.redouanebali.controller;

import io.github.redouanebali.model.Player;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/players")
public class PlayerController {

  @GetMapping
  public List<Player> getAllPlayers() {
    List<Player> players = new ArrayList<>();
    players.add(new Player(1L, "John Doe", 5, 1200, 1990));
    players.add(new Player(2L, "Jane Smith", 7, 950, 1992));
    return players;
  }
}
