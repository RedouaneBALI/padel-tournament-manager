package io.github.redouanebali.service;

import io.github.redouanebali.dto.ScoreUpdateResponse;
import io.github.redouanebali.dto.SimplePlayerPairDTO;
import io.github.redouanebali.generation.KnockoutRoundGenerator;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.Stage;
import io.github.redouanebali.model.TeamSide;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.repository.GameRepository;
import io.github.redouanebali.repository.MatchFormatRepository;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.repository.PlayerRepository;
import io.github.redouanebali.repository.RoundRepository;
import io.github.redouanebali.repository.ScoreRepository;
import io.github.redouanebali.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Setter
public class TournamentService {

  @Autowired
  private PlayerPairRepository         playerPairRepository;
  @Autowired
  private PlayerRepository             playerRepository;
  @Autowired
  private TournamentRepository         tournamentRepository;
  @Autowired
  private RoundRepository              roundRepository;
  @Autowired
  private GameRepository               gameRepository;
  @Autowired
  private ScoreRepository              scoreRepository;
  @Autowired
  private MatchFormatRepository        matchFormatRepository;
  @Autowired
  private TournamentProgressionService progressionService;

  public Tournament getTournamentById(Long id) {
    return tournamentRepository.findById(id)
                               .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
  }

  public Tournament createTournament(final Tournament tournament) {
    if (tournament == null) {
      throw new IllegalArgumentException("Tournament cannot be null");
    }
    Tournament saved = tournamentRepository.save(tournament);

    if (saved.getNbMaxPairs() <= 1) {
      return saved;
    }

    LinkedHashSet<Round> rounds  = new LinkedHashSet<>();
    Stage                current = Stage.fromNbTeams(saved.getNbMaxPairs());

    while (current != null && current != Stage.WINNER) {
      Round round = new Round(current);

      MatchFormat matchFormat = round.getMatchFormat();
      if (matchFormat != null && matchFormat.getId() == null) {
        matchFormat = matchFormatRepository.save(matchFormat);
        round.setMatchFormat(matchFormat);
      }

      int nbMatches = current.getNbTeams() / 2;

      List<Game> games = new ArrayList<>();
      for (int i = 0; i < nbMatches; i++) {
        Game game = new Game(matchFormat);
        games.add(game);
        gameRepository.save(game);
      }

      round.setGames(games);
      roundRepository.save(round);
      rounds.add(round);

      current = current.next();
    }

    saved.setRounds(rounds);
    return tournamentRepository.save(saved);
  }

  public Tournament updateTournament(Long tournamentId, Tournament updatedTournament) {
    Tournament existing = getTournamentById(tournamentId);

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
    Tournament tournament = getTournamentById(tournamentId);

    tournament.getPlayerPairs().clear();
    tournamentRepository.save(tournament);

    List<PlayerPair> newPairs = playerPairsDto.stream().map(dto -> {
      Player p1 = new Player();
      p1.setName(dto.getPlayer1());

      Player p2 = new Player();
      p2.setName(dto.getPlayer2());

      playerRepository.save(p1);
      playerRepository.save(p2);

      PlayerPair pair = new PlayerPair(null, p1, p2, dto.getSeed());
      return playerPairRepository.save(pair);
    }).toList();

    tournament.getPlayerPairs().addAll(newPairs);
    tournamentRepository.save(tournament);

    return newPairs.size();
  }

  public Tournament generateDraw(Long tournamentId) {
    Tournament tournament = getTournamentById(tournamentId);

    List<PlayerPair> pairs        = new ArrayList<>(tournament.getPlayerPairs());
    int              originalSize = pairs.size();
    int              powerOfTwo   = 1;
    while (powerOfTwo < originalSize) {
      powerOfTwo *= 2;
    }
    int missing = powerOfTwo - originalSize;

    for (int i = 0; i < missing; i++) {
      PlayerPair bye = PlayerPair.bye();
      persistPairIfNeeded(bye);
      pairs.add(bye);
    }

    KnockoutRoundGenerator generator = new KnockoutRoundGenerator(pairs, tournament.getNbSeeds());
    Round                  newRound  = generator.generate();

    Round existingRound = tournament.getRounds().stream()
                                    .filter(r -> r.getStage().equals(newRound.getStage()))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("Round not found"));

    List<Game> existingGames = existingRound.getGames();
    for (int i = 0; i < existingGames.size() && i < newRound.getGames().size(); i++) {
      Game existingGame = existingGames.get(i);
      Game newGame      = newRound.getGames().get(i);
      existingGame.setTeamA(newGame.getTeamA());
      existingGame.setTeamB(newGame.getTeamB());
      gameRepository.save(existingGame);
    }

    roundRepository.save(existingRound);
    tournament.setRounds(new LinkedHashSet<>(tournament.getRounds()));
    new TournamentProgressionService().propagateWinners(tournament);

    return tournamentRepository.save(tournament);
  }

  private PlayerPair persistPairIfNeeded(PlayerPair pair) {
    if (pair == null) {
      return null;
    }

    Player p1 = pair.getPlayer1();
    Player p2 = pair.getPlayer2();

    if (p1 != null && p1.getId() == null) {
      p1 = playerRepository.save(p1);
    }
    if (p2 != null && p2.getId() == null) {
      p2 = playerRepository.save(p2);
    }

    pair.setPlayer1(p1);
    pair.setPlayer2(p2);

    if (pair.getId() == null) {
      pair = playerPairRepository.save(pair);
    }

    return pair;
  }

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

    MatchFormat persistedFormat = matchFormatRepository.save(newFormat);
    round.setMatchFormat(persistedFormat);
    roundRepository.save(round);
    return persistedFormat;
  }

  public ScoreUpdateResponse updateGameScore(Long tournamentId, Long gameId, Score score) {
    Game       game       = getGameById(gameId);
    Tournament tournament = getTournamentById(tournamentId);

    boolean belongsToTournament = tournament.getRounds().stream()
                                            .flatMap(round -> round.getGames().stream())
                                            .anyMatch(g -> g.getId().equals(gameId));

    if (!belongsToTournament) {
      throw new IllegalArgumentException("Game does not belong to the tournament");
    }

    Score persistedScore = scoreRepository.save(score);
    game.setScore(persistedScore);
    gameRepository.save(game);

    TeamSide winner = null;
    if (game.isFinished()) {
      progressionService.propagateWinners(tournament);
      tournamentRepository.save(tournament);

      winner = progressionService.getWinner(game).equals(game.getTeamA())
               ? TeamSide.TEAM_A
               : TeamSide.TEAM_B;

      return new ScoreUpdateResponse(true, winner);
    }

    return new ScoreUpdateResponse(false, null);
  }

  public Game getGameById(Long gameId) {
    return gameRepository.findById(gameId)
                         .orElseThrow(() -> new IllegalArgumentException("Game not found"));
  }

  public Set<Game> getGamesByTournamentAndStage(Long tournamentId, Stage stage) {
    Tournament tournament = getTournamentById(tournamentId);

    Round round = tournament.getRounds().stream()
                            .filter(r -> r.getStage() == stage)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Round not found for stage: " + stage));

    return new LinkedHashSet<>(round.getGames());
  }
}
