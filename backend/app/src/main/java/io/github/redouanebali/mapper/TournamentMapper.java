package io.github.redouanebali.mapper;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.request.CreateTournamentRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.PoolRankingDetailsDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.RoundLightDTO;
import io.github.redouanebali.dto.response.ScoreDTO;
import io.github.redouanebali.dto.response.SetScoreDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.dto.response.TournamentSummaryDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.PairType;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.PoolRanking;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Score;
import io.github.redouanebali.model.SetScore;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.security.SecurityUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TournamentMapper {

  @Mapping(target = "organizerName", ignore = true)
  @Mapping(target = "isEditable", expression = "java(tournament.isEditableBy(io.github.redouanebali.security.SecurityUtil.currentUserId()))")
  @Mapping(target = "editorIds", source = "editorIds")
  TournamentDTO toDTOBase(Tournament tournament);

  default TournamentDTO toDTO(Tournament tournament) {
    return toDTOBase(tournament);
  }

  default List<TournamentDTO> toDTO(List<Tournament> tournaments) {
    if (tournaments == null) {
      return new ArrayList<>();
    }
    return tournaments.stream().map(this::toDTO).toList();
  }

  default Set<TournamentDTO> toDTO(Set<Tournament> tournaments) {
    if (tournaments == null) {
      return Set.of();
    }
    return tournaments.stream().map(this::toDTO).collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Converts a Tournament to TournamentDTO with isEditable flag based on user permissions. Uses AuthorizationService to determine edit rights (owner,
   * editor, or super-admin).
   *
   * @param tournament the tournament entity
   * @param userId the current user ID
   * @param canEdit whether the user can edit this tournament
   * @return TournamentDTO with isEditable flag set
   */
  default TournamentDTO toDTOWithEditPermission(Tournament tournament, String userId, boolean canEdit) {
    if (tournament == null) {
      return null;
    }
    TournamentDTO dto = toDTO(tournament);
    dto.setIsEditable(canEdit);
    return dto;
  }

  /**
   * Converts a list of tournaments to DTOs with isEditable flag for each.
   *
   * @param tournaments the list of tournament entities
   * @param userId the current user ID
   * @param authService the authorization service to check permissions
   * @return list of TournamentDTOs with isEditable flags set
   */
  default List<TournamentDTO> toDTOWithEditPermission(
      List<Tournament> tournaments,
      String userId,
      io.github.redouanebali.security.AuthorizationService authService
  ) {
    if (tournaments == null) {
      return new ArrayList<>();
    }
    List<TournamentDTO> dtos = new ArrayList<>();
    for (Tournament t : tournaments) {
      boolean canEdit = authService.canEditTournament(t, userId);
      dtos.add(toDTOWithEditPermission(t, userId, canEdit));
    }
    return dtos;
  }

  @Mapping(target = "round", ignore = true)
  @Mapping(target = "isEditable", ignore = true)
  GameDTO toDTO(Game game);

  List<GameDTO> toDTOGameList(List<Game> games);

  Set<GameDTO> toDTOGameSet(Set<Game> games);

  /**
   * Maps a Game to GameDTO with the associated Round included. This method is used when detailed round information is needed alongside the game
   * data.
   *
   * @param game the game entity to map
   * @param round the round entity that contains this game
   * @return GameDTO with round information included
   */
  default GameDTO toDTOWithRound(Game game, Round round) {
    if (game == null) {
      return null;
    }
    GameDTO gameDTO = toDTO(game);
    if (round != null) {
      RoundLightDTO light = new RoundLightDTO(round.getId(), round.getStage(), toDTO(round.getMatchFormat()));
      gameDTO.setRound(light);
    }
    return gameDTO;
  }

  /**
   * Maps a Game to GameDTO with a simplified Round (id, stage, matchFormat only).
   *
   * @param game the game entity to map
   * @param round the round entity that contains this game
   * @return GameDTO with simplified round information
   */
  default GameDTO toDTOWithLightRound(Game game, Round round) {
    if (game == null) {
      return null;
    }
    GameDTO gameDTO = toDTO(game);
    if (round != null) {
      RoundLightDTO light = new RoundLightDTO();
      light.setId(round.getId());
      light.setStage(round.getStage());
      light.setMatchFormat(toDTO(round.getMatchFormat()));
      gameDTO.setRound(light);
    }
    return gameDTO;
  }

  default PlayerPairDTO toDTO(PlayerPair playerPair) {
    if (playerPair == null) {
      return null;
    }

    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(playerPair.getId());
    dto.setType(playerPair.getType());

    // Case 1: QUALIFIER (placeholder for a pair that will come from qualifications)
    if (playerPair.isQualifier()) {
      dto.setQualifierSlot(true);
      // Important: NO player names for qualifiers
      // The frontend will display just "(Q1)", "(Q2)", etc. via displaySeed
      dto.setPlayer1Name(null);
      dto.setPlayer2Name(null);
      // Get the qualifier number from the dedicated qualifierIndex field
      Integer qualifierIndex = playerPair.getQualifierIndex();
      String  qualifierName  = qualifierIndex != null ? "Q" + qualifierIndex : "Q";
      dto.setDisplaySeed(qualifierName); // Display "Q1", "Q2", etc. for frontend to show "(Q1)", "(Q2)"
      return dto;
    }

    // Case 2: BYE (automatic bye)
    if (playerPair.isBye()) {
      dto.setBye(true);
      dto.setDisplaySeed(null);
      // BYEs can keep their names "BYE" / "BYE" if necessary
      dto.setPlayer1Name(playerPair.getPlayer1() != null ? playerPair.getPlayer1().getName() : null);
      dto.setPlayer2Name(playerPair.getPlayer2() != null ? playerPair.getPlayer2().getName() : null);
      return dto;
    }

    // Case 3: NORMAL pair (with real players)
    dto.setPlayer1Name(playerPair.getPlayer1() != null ? playerPair.getPlayer1().getName() : null);
    dto.setPlayer2Name(playerPair.getPlayer2() != null ? playerPair.getPlayer2().getName() : null);
    dto.setSeed(playerPair.getSeed());

    // Display seed for UI
    if (playerPair.getSeed() > 0 && playerPair.getSeed() < Integer.MAX_VALUE) {
      dto.setDisplaySeed(String.valueOf(playerPair.getSeed()));
    } else {
      dto.setDisplaySeed(null);
    }

    return dto;
  }

  List<PlayerPairDTO> toDTOPlayerPairList(List<PlayerPair> playerPairs);

  Set<PlayerPairDTO> toDTOPlayerPairSet(Set<PlayerPair> playerPairs);

  RoundDTO toDTO(Round round);

  List<RoundDTO> toDTORoundList(List<Round> rounds);

  MatchFormatDTO toDTO(MatchFormat matchFormat);

  List<MatchFormatDTO> toDTOMatchFormatList(List<MatchFormat> matchFormats);

  Set<MatchFormatDTO> toDTOMatchFormatSet(Set<MatchFormat> matchFormats);

  // Mapping Pool -> PoolDTO
  @Mapping(target = "pairs", source = "pairs")
  @Mapping(target = "poolRanking", source = "poolRanking")
  PoolDTO toDTO(Pool pool);

  List<PoolDTO> toDTOPoolList(List<io.github.redouanebali.model.Pool> pools);

  // Mapping PoolRanking -> PoolRankingDTO
  @Mapping(target = "id", source = "id")
  @Mapping(target = "details", source = "details")
  PoolRankingDTO toDTO(PoolRanking poolRanking);

  List<PoolRankingDTO> toDTOPoolRankingList(List<PoolRanking> poolRankings);

  @Mapping(target = "pairId", source = "playerPair.id")
  PoolRankingDetailsDTO toDTO(io.github.redouanebali.model.PoolRankingDetails details);

  List<PoolRankingDetailsDTO> toDTOPoolRankingDetailsList(List<io.github.redouanebali.model.PoolRankingDetails> details);

  List<PlayerPair> toPlayerPairList(List<CreatePlayerPairRequest> requests);

  default Player toPlayer(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    return new Player(name);
  }

  default PlayerPair toPlayerPair(CreatePlayerPairRequest request) {
    if (request == null) {
      return null;
    }

    PlayerPair pair = new PlayerPair();
    pair.setPlayer1(toPlayer(request.getPlayer1Name()));
    pair.setPlayer2(toPlayer(request.getPlayer2Name()));
    pair.setType(request.getType() != null ? request.getType() : PairType.NORMAL);
    if (request.getType() == PairType.BYE) {
      pair.setSeed(Integer.MAX_VALUE);
    } else {
      pair.setSeed(request.getSeed() != null ? request.getSeed() : 0);
    }
    return pair;
  }

  default boolean isEditable(Tournament tournament) {
    String me = SecurityUtil.currentUserId();
    if (me == null) {
      return false; // user not authenticated
    }
    // Rely on Tournament.isEditableBy which checks ownerId and editorIds
    // Super-admin checks are performed at service/controller layer where SecurityProps is available
    return tournament != null && tournament.isEditableBy(me);
  }

  /**
   * Converts a CreateTournamentRequest DTO to a Tournament entity. Maps only the fields that can be set during creation, excluding system-managed
   * fields.
   *
   * @param request the tournament creation request
   * @return a new Tournament entity with mapped fields
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ownerId", ignore = true)
  @Mapping(target = "rounds", ignore = true)
  @Mapping(target = "playerPairs", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "editorIds", source = "editorIds")
  Tournament toEntity(CreateTournamentRequest request);

  // Summary mapping for the public home endpoint
  TournamentSummaryDTO toSummaryDTO(Tournament tournament);

  default ScoreDTO toDTO(Score score) {
    if (score == null) {
      return null;
    }
    ScoreDTO          dto     = new ScoreDTO();
    List<SetScoreDTO> setsDto = toDTOSets(score.getSets());
    dto.setSets(setsDto);
    dto.setForfeit(score.isForfeit());
    dto.setForfeitedBy(score.getForfeitedBy());
    if (score.getCurrentGamePointA() != null) {
      dto.setCurrentGamePointA(score.getCurrentGamePointA().name());
    }
    if (score.getCurrentGamePointB() != null) {
      dto.setCurrentGamePointB(score.getCurrentGamePointB().name());
    }
    if (score.getTieBreakPointA() != null) {
      dto.setTieBreakPointA(score.getTieBreakPointA());
    }
    if (score.getTieBreakPointB() != null) {
      dto.setTieBreakPointB(score.getTieBreakPointB());
    }
    return dto;
  }

  default ScoreDTO toDTO(Game game, Score score) {
    if (score == null) {
      return null;
    }
    ScoreDTO          dto     = new ScoreDTO();
    List<SetScoreDTO> setsDto = toDTOSets(score.getSets());
    // Only inject tieBreakPointA/B in last set if we are in a super tie-break scenario
    if (game != null && game.getFormat() != null && game.getFormat().isSuperTieBreakInFinalSet()) {
      int setsToWin        = game.getFormat().getNumberOfSetsToWin();
      int expectedSetCount = setsToWin * 2 - 1;
      if (setsDto.size() == expectedSetCount && score.getTieBreakPointA() != null && score.getTieBreakPointB() != null) {
        SetScoreDTO lastSet = setsDto.get(setsDto.size() - 1);
        lastSet.setTieBreakTeamA(score.getTieBreakPointA());
        lastSet.setTieBreakTeamB(score.getTieBreakPointB());
      }
    }
    dto.setSets(setsDto);
    dto.setForfeit(score.isForfeit());
    dto.setForfeitedBy(score.getForfeitedBy());
    if (score.getCurrentGamePointA() != null) {
      dto.setCurrentGamePointA(score.getCurrentGamePointA().name());
    }
    if (score.getCurrentGamePointB() != null) {
      dto.setCurrentGamePointB(score.getCurrentGamePointB().name());
    }
    if (score.getTieBreakPointA() != null) {
      dto.setTieBreakPointA(score.getTieBreakPointA());
    }
    if (score.getTieBreakPointB() != null) {
      dto.setTieBreakPointB(score.getTieBreakPointB());
    }
    return dto;
  }

  default List<SetScoreDTO> toDTOSets(List<SetScore> sets) {
    if (sets == null) {
      return new ArrayList<>();
    }
    List<SetScoreDTO> result = new ArrayList<>();
    for (SetScore set : sets) {
      SetScoreDTO dto = new SetScoreDTO();
      dto.setTeamAScore(set.getTeamAScore());
      dto.setTeamBScore(set.getTeamBScore());
      Integer tieBreakA = set.getTieBreakTeamA();
      dto.setTieBreakTeamA(tieBreakA);
      Integer tieBreakB = set.getTieBreakTeamB();
      dto.setTieBreakTeamB(tieBreakB);
      result.add(dto);
    }
    return result;
  }
}
