package io.github.redouanebali.service;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.repository.PlayerRepository;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  @Autowired
  private PlayerPairRepository playerPairRepository;
  @Autowired
  private PlayerRepository     playerRepository;
  @Autowired
  private TournamentRepository tournamentRepository;
  @Autowired
  private RoundRepository      roundRepository;
  @Autowired
  private GameRepository       gameRepository;

  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }

  public Tournament createTournament(final Tournament tournament) {
    System.out.println("TournamentService.createTournament");
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    return tournamentRepository.save(tournament);
  }

  public Tournament updateTournament(Long tournamentId, Tournament updatedTournament) {
    Tournament existing = tournamentRepository.findById(tournamentId)
                                              .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    existing.setName(updatedTournament.getName());
    existing.setStartDate(updatedTournament.getStartDate());
    existing.setEndDate(updatedTournament.getEndDate());
    existing.setDescription(updatedTournament.getDescription());
    existing.setCity(updatedTournament.getCity());
    existing.setClub(updatedTournament.getClub());
    existing.setGender(updatedTournament.getGender());
    existing.setLevel(updatedTournament.getLevel());
    existing.setTournamentFormat(updatedTournament.getTournamentFormat());
    existing.setNbSeeds(updatedTournament.getNbSeeds());
    existing.setNbMaxPairs(updatedTournament.getNbMaxPairs());

    return tournamentRepository.save(existing);
  }

  public int addPairs(Long tournamentId, List<SimplePlayerPairDTO> playerPairsDto) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    // Supprimer les anciennes paires
    tournament.getPlayerPairs().clear();
    tournamentRepository.save(tournament); // Nécessaire si orphanRemoval = true

    List<PlayerPair> newPairs = playerPairsDto.stream().map(dto -> {
      Player p1 = new Player();
      p1.setName(dto.getPlayer1());

      Player p2 = new Player();
      p2.setName(dto.getPlayer2());

      playerRepository.save(p1);
      playerRepository.save(p2);

      PlayerPair pair = new PlayerPair(null, p1, p2, dto.getSeed());

      // Sauvegarde immédiate
      return playerPairRepository.save(pair);
    }).toList();

    // Ajout des paires désormais persistées
    tournament.getPlayerPairs().addAll(newPairs);

    tournamentRepository.save(tournament);

    return newPairs.size();
  }

  private String normalize(String name) {
    return name == null ? "" : name.trim().toLowerCase();
  }

  public Round generateDraw(Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    List<PlayerPair> pairs = new ArrayList<>(tournament.getPlayerPairs());

    // 🔧 Ajouter les BYE manuellement
    int originalSize = pairs.size();
    int powerOfTwo   = 1;
    while (powerOfTwo < originalSize) {
      powerOfTwo *= 2;
    }
    int missing = powerOfTwo - originalSize;

    for (int i = 0; i < missing; i++) {
      PlayerPair bye = PlayerPair.bye();
      persistPairIfNeeded(bye);
      pairs.add(bye);
    }

    // ✅ Toutes les paires sont maintenant persistées
    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(pairs, tournament.getNbSeeds());
    Round                  round     = generator.generate();

    // Enregistrer les games
    for (Game game : round.getGames()) {
      gameRepository.save(game);
    }

    roundRepository.save(round);
    tournament.getRounds().add(round);
    tournamentRepository.save(tournament);

    return round;
  }

  private PlayerPair persistPairIfNeeded(PlayerPair pair) {
    if (pair == null) {
      return null;
    }

    Player p1 = pair.getPlayer1();
    Player p2 = pair.getPlayer2();

    // Sauvegarde les joueurs s’ils ne le sont pas encore
    if (p1 != null && p1.getId() == null) {
      p1 = playerRepository.save(p1);
    }

    if (p2 != null && p2.getId() == null) {
      p2 = playerRepository.save(p2);
    }

    // Mets à jour les références dans la paire
    pair.setPlayer1(p1);
    pair.setPlayer2(p2);

    // Sauvegarde la paire si nécessaire
    if (pair.getId() == null) {
      pair = playerPairRepository.save(pair);
    }

    return pair;
  }
}
