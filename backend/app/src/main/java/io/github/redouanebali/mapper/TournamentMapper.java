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
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
import io.github.redouanebali.model.Pool;
import io.github.redouanebali.model.PoolRanking;
import io.github.redouanebali.model.Round;
import io.github.redouanebali.model.Tournament;
import io.github.redouanebali.security.SecurityUtil;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TournamentMapper {
  
  TournamentDTO toDTO(Tournament tournament);

  List<TournamentDTO> toDTO(List<Tournament> tournaments);

  Set<TournamentDTO> toDTO(Set<Tournament> tournaments);

  GameDTO toDTO(Game game);

  List<GameDTO> toDTOGameList(List<Game> games);

  Set<GameDTO> toDTOGameSet(Set<Game> games);

  default PlayerPairDTO toDTO(PlayerPair playerPair) {
    if (playerPair == null) {
      return null;
    }

    PlayerPairDTO dto = new PlayerPairDTO();
    dto.setId(playerPair.getId());
    dto.setType(playerPair.getType());

    // Cas 1 : C'est un QUALIFIER (placeholder pour une paire qui viendra des qualifications)
    if (playerPair.isQualifier()) {
      dto.setQualifierSlot(true);
      dto.setDisplaySeed("Q");
      // Important : PAS de noms de joueurs pour les qualifiers
      // Le front affichera juste "Q"
      return dto;
    }

    // Cas 2 : C'est un BYE (bye automatique)
    if (playerPair.isBye()) {
      dto.setBye(true);
      dto.setDisplaySeed(null);
      // Les BYE peuvent garder leurs noms "BYE" / "BYE" si nécessaire
      dto.setPlayer1Name(playerPair.getPlayer1() != null ? playerPair.getPlayer1().getName() : null);
      dto.setPlayer2Name(playerPair.getPlayer2() != null ? playerPair.getPlayer2().getName() : null);
      return dto;
    }

    // Cas 3 : Paire NORMALE (avec de vrais joueurs)
    dto.setPlayer1Name(playerPair.getPlayer1() != null ? playerPair.getPlayer1().getName() : null);
    dto.setPlayer2Name(playerPair.getPlayer2() != null ? playerPair.getPlayer2().getName() : null);
    dto.setSeed(playerPair.getSeed());

    // Display seed pour l'UI
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
    pair.setSeed(request.getSeed() != null ? request.getSeed() : 0); // Assigner la seed
    return pair;
  }

  default boolean isEditable(Tournament tournament) {
    String me = SecurityUtil.currentUserId();
    if (me == null) {
      return false; // utilisateur non connecté
    }
    return SecurityUtil.isSuperAdmin(Set.of(me))
           || (tournament.getOwnerId() != null && tournament.getOwnerId().equals(me));
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
  Tournament toEntity(CreateTournamentRequest request);
}