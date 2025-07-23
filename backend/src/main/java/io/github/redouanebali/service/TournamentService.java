package io.github.redouanebali.service;

import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.GroupStageRoundGenerator;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.generation.RoundGenerator;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentFormat;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {

  private final Map<TournamentFormat, RoundGenerator> generators = Map.of(
      TournamentFormat.KNOCKOUT, new KnockoutRoundGenerator(),
      TournamentFormat.GROUP_STAGE, new GroupStageRoundGenerator()
  );
  @Autowired
  private       TournamentRepository                  tournamentRepository;
  @Autowired
  private RoundRepository      roundRepository;
  @Autowired
  private PlayerPairRepository playerPairRepository;

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

  public int addPairs(final Long tournamentId, final List<SimplePlayerPairDTO> playerPairsDto) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
                                                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

    // Création d’un Set des identifiants uniques des paires déjà existantes
    Set<String> existingPairs = tournament.getPlayerPairs().stream()
                                          .map(pair -> normalize(pair.getPlayer1().getName()) + "-" + normalize(pair.getPlayer2().getName()))
                                          .collect(Collectors.toSet());

    // Filtrer les nouvelles paires pour ne pas inclure celles déjà existantes
    List<PlayerPair> newPairs = playerPairsDto.stream()
                                              .map(SimplePlayerPairDTO::toPlayerPair)
                                              .filter(pair -> {
                                                String key = normalize(pair.getPlayer1().getName()) + "-" + normalize(pair.getPlayer2().getName());
                                                return !existingPairs.contains(key);
                                              })
                                              .toList();

    // Sauvegarde uniquement des nouvelles paires
    playerPairRepository.saveAll(newPairs);
    tournament.getPlayerPairs().addAll(newPairs);
    tournamentRepository.save(tournament);

    return tournament.getPlayerPairs().size();
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

    roundRepository.save(round);

    return round;
  }
}
