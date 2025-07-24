package io.github.redouanebali.service;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.GroupStageRoundGenerator;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.generation.RoundGenerator;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.repository.PlayerRepository;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  private final Map<TournamentFormat, RoundGenerator> generators = Map.of(
      TournamentFormat.KNOCKOUT, new KnockoutRoundGenerator(),
      TournamentFormat.GROUP_STAGE, new GroupStageRoundGenerator()
  );
  @Autowired
  private       PlayerPairRepository                  playerPairRepository;
  @Autowired
  private       PlayerRepository                      playerRepository;
  @Autowired
  private       TournamentRepository                  tournamentRepository;
  @Autowired
  private       RoundRepository                       roundRepository;
  @Autowired
  private       GameRepository                        gameRepository;

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

  public Round generateDraw(final Long tournamentId) {
    System.out.println("TournamentService.generateDraw");
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    RoundGenerator generator = new KnockoutRoundGenerator();
    if (tournament.getTournamentFormat() != null) {
      generator = generators.get(tournament.getTournamentFormat());
    } else {
      generator = new KnockoutRoundGenerator();
    }
    Round round = generator.generate(tournament.getPlayerPairs(), tournament.getNbSeeds());
    round.getGames().forEach(g -> gameRepository.save(g));
    roundRepository.save(round);
    tournament.setRounds(new ArrayList<>());
    tournament.getRounds().add(round);
    tournamentRepository.save(tournament);
    return round;
  }
}
