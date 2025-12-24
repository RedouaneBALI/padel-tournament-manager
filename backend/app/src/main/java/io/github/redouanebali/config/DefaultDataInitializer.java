package io.github.redouanebali.config;

import io.github.redouanebali.dto.request.GameRequest;
import io.github.redouanebali.dto.request.PlayerPairRequest;
import io.github.redouanebali.dto.request.RoundRequest;
import io.github.redouanebali.generation.TournamentBuilder;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.model.TournamentLevel;
import io.github.redouanebali.model.User;
import io.github.redouanebali.model.format.TournamentConfig;
import io.github.redouanebali.model.format.TournamentFormat;
import io.github.redouanebali.repository.PlayerPairRepository;
import io.github.redouanebali.repository.TournamentRepository;
import io.github.redouanebali.repository.UserRepository;
import io.github.redouanebali.service.DrawGenerationService;
import io.github.redouanebali.service.TournamentService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultDataInitializer implements CommandLineRunner {

  private static final String                DEFAULT_USER_EMAIL = "bali.redouane@gmail.com";
  private static final String
                                             PLAYER_LIST        =
      "SEDKI MESSAOURI ABDELALI,TAGHY ZAKARIA,107 SEFRAOUI OTHMANE,ZARHLOULE SALIM,365 ALAMI YASSINE,RHIATI KAMAL,238 MRINI ADIL,BOUSSMANE MUSTAPHA,216 BENJELLOUN MHAMED,DAMIEL MALDONADO,198 TAHA MEHDI,BOUAOUDA ANASS,142 BERRADA GHALI,BOUZOUBAA MOHAMED,133 EL KENDOUCI EL MEHDI,EL KENDOUCI DRISS,396 BERRAHOU HAMZA,MUESSER YANIS,666 ALAOUI HAMZA,LAVAUD ERIC,245 SAGARD FABIEN,TOUHAMI YASSINE,194 FREJ WIFAQ,LAKHDAR GHAZAL ANAS,138 EL GORFTI ISMAEL,DAHMANI ADIL,143 SERGHINI REDA,EL GHADRAOUI HICHAM,235 BOUHMADI MEHDI,RIZO AVILA BRAULIO,206 JAKHOUKH ANOUAR,CHADLI AMINE,280 ALLA YAHYA,SKALLI MEHDI,247 BENNANI ZIATNI MOHAMED,BENNANI ZIATNI OTHMANE,260 SEDKI RAYANE,SBAI JASSIME,121 KHTIRA MEHDI,ALAOUI MOULAY,151 RHAOUTI YAHYA MOHAMED,ARROUB MEHDI,623 SAOURI HAMZA,BENZAKOUR ADIL,211 BERRADA HICHAM,SLAOUI HAMZA,387 TAHOUR ALI,AMMARI YAHIA,548 BENNIS MOHAMED,KHOUCHOU BADR,551 EL GALLAF OUSSAMA,EL FRIYAKH ZAKARYAE,119 FERTAT REDA,PASTOR LANDABURU PABLO,12 TAZI DRISS,HASSOUNI ABDESLAM,95 MENJRA MAHMOUD,EL KHARROUBI MONCEF,62 MEKOUAR SAAD,SAISSI MEHDI,35 HICHAM IDRISS,MECHICHE ALAMI MAMOUN,25 JACQUETY PIERRE LOUIS,LEROUX TANGUY";
  private final        UserRepository        userRepository;
  private final        TournamentRepository  tournamentRepository;
  private final        PlayerPairRepository  playerPairRepository;
  private final        Environment           environment;
  private final        TournamentService     tournamentService;
  private final        DrawGenerationService drawGenerationService;

  @Override
  public void run(String... args) throws Exception {
    log.info("Active profiles: {}", Arrays.asList(environment.getActiveProfiles()));
    if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
      log.info("Production profile active, skipping default data initialization");
      return;
    }
    log.info("Not production, proceeding with initialization");
    try {
      if (tournamentRepository.findAllByOwnerId(DEFAULT_USER_EMAIL).isEmpty()) {
        log.info("Initializing default tournament for user {}", DEFAULT_USER_EMAIL);
        createDefaultTournament();
      } else {
        log.info("Default tournament already exists for user {}", DEFAULT_USER_EMAIL);
      }
    } catch (Exception e) {
      log.error("Error initializing default data", e);
    }
  }

  private void createDefaultTournament() {
    // Create user if not exists
    User user = userRepository.findByEmail(DEFAULT_USER_EMAIL).orElseGet(() -> {
      User newUser = new User(DEFAULT_USER_EMAIL, "Bali Redouane", "en");
      return userRepository.save(newUser);
    });

    // Parse players
    List<Player>     players = parsePlayers();
    List<PlayerPair> pairs   = createPairs(players);

    // Don't save pairs separately, let cascade handle it

    // Create tournament
    Tournament tournament = new Tournament();
    tournament.getPlayerPairs().addAll(pairs);
    tournament.setName("Default Tournament");
    tournament.setGender(Gender.MEN);
    tournament.setLevel(TournamentLevel.P1500);
    tournament.setClub("Oasis City Ball");
    tournament.setCity("Casablanca");
    tournament.setOwnerId(DEFAULT_USER_EMAIL);
    tournament.setStartDate(LocalDate.now().minusDays(1));
    tournament.setEndDate(LocalDate.now().plusDays(1));

    TournamentConfig config = TournamentConfig.builder()
                                              .format(TournamentFormat.KNOCKOUT)
                                              .mainDrawSize(32)
                                              .nbSeeds((int) players.stream().filter(p -> p.getRanking() != null && p.getRanking() > 0).count())
                                              .build();
    tournament.setConfig(config);

    // Initialize rounds
    TournamentBuilder.initializeEmptyRounds(tournament);

    // Create games with teams
    Round      firstRound = tournament.getRounds().get(0);
    List<Game> newGames   = new ArrayList<>();
    for (int i = 0; i < pairs.size() / 2; i++) {
      Game game = new Game();
      game.setTeamA(pairs.get(i * 2));
      game.setTeamB(pairs.get(i * 2 + 1));
      game.setFormat(firstRound.getMatchFormat());
      newGames.add(game);
    }
    firstRound.replaceGames(newGames);

    // Save tournament
    tournamentRepository.save(tournament);
    // No need to reload, ids are set on the object
    log.info("Default tournament created with {} pairs", pairs.size());

    // Generate draw
    try {
      // Set temporary security context for authorization
      Jwt jwt = Jwt.withTokenValue("token")
                   .header("alg", "RS256")
                   .claim("email", DEFAULT_USER_EMAIL)
                   .build();
      var auth    = new JwtAuthenticationToken(jwt);
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(auth);
      SecurityContextHolder.setContext(context);

      // Create RoundRequest from the first round
      RoundRequest roundRequest = new RoundRequest();
      roundRequest.setStage(firstRound.getStage().name());
      List<GameRequest> gameRequests = new ArrayList<>();
      for (Game game : firstRound.getGames()) {
        GameRequest gReq = new GameRequest();
        if (game.getTeamA() != null) {
          gReq.setTeamA(PlayerPairRequest.fromModel(game.getTeamA()));
        }
        if (game.getTeamB() != null) {
          gReq.setTeamB(PlayerPairRequest.fromModel(game.getTeamB()));
        }
        gameRequests.add(gReq);
      }
      roundRequest.setGames(gameRequests);
      tournament = tournamentService.generateDrawManual(tournament.getId(), List.of(roundRequest));
      // Save again if modified
      tournamentRepository.save(tournament);
      log.info("Draw generated for default tournament");

      // Clear security context
      SecurityContextHolder.clearContext();
    } catch (Exception e) {
      log.warn("Failed to generate draw for default tournament", e);
      // Clear security context in case of error
      SecurityContextHolder.clearContext();
    }
  }

  private List<Player> parsePlayers() {
    List<Player> players = new ArrayList<>();
    String[]     names   = PLAYER_LIST.split(",");
    Pattern      pattern = Pattern.compile("^(\\d+)\\s+(.+)$");
    for (String name : names) {
      name = name.trim();
      Matcher matcher = pattern.matcher(name);
      Integer seed    = null;
      String  playerName;
      if (matcher.matches()) {
        seed       = Integer.parseInt(matcher.group(1));
        playerName = matcher.group(2);
      } else {
        playerName = name;
      }
      Player player = Player.builder()
                            .name(playerName)
                            .ranking(seed)
                            .points(0)
                            .birthYear(1990)
                            .build();
      players.add(player);
    }
    return players;
  }

  private List<PlayerPair> createPairs(List<Player> players) {
    List<PlayerPair> pairs = new ArrayList<>();
    for (int i = 0; i < players.size(); i += 2) {
      Player p1 = players.get(i);
      Player p2 = players.get(i + 1);
      int seed = Math.min(p1.getRanking() != null ? p1.getRanking() : Integer.MAX_VALUE,
                          p2.getRanking() != null ? p2.getRanking() : Integer.MAX_VALUE);
      if (seed == Integer.MAX_VALUE) {
        seed = 0;
      }
      PlayerPair pair = new PlayerPair(p1, p2, seed);
      pairs.add(pair);
    }
    return pairs;
  }
}
