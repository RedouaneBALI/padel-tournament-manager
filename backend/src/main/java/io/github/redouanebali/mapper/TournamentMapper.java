package io.github.redouanebali.mapper;

import io.github.redouanebali.dto.request.CreatePlayerPairRequest;
import io.github.redouanebali.dto.response.GameDTO;
import io.github.redouanebali.dto.response.MatchFormatDTO;
import io.github.redouanebali.dto.response.PlayerPairDTO;
import io.github.redouanebali.dto.response.PoolRankingDTO;
import io.github.redouanebali.dto.response.RoundDTO;
import io.github.redouanebali.dto.response.TournamentDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.MatchFormat;
import io.github.redouanebali.model.Player;
import io.github.redouanebali.model.PlayerPair;
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

  @Mapping(target = "rounds", source = "rounds")
  @Mapping(target = "playerPairs", source = "playerPairs")
  @Mapping(target = "editable", expression = "java(isEditable(tournament))")
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
    dto.setSeed(playerPair.getSeed());
    dto.setBye(playerPair.isBye());
    dto.setPlayer1Name(playerPair.getPlayer1() != null ? playerPair.getPlayer1().getName() : null);
    dto.setPlayer2Name(playerPair.getPlayer2() != null ? playerPair.getPlayer2().getName() : null);
    return dto;
  }

  List<PlayerPairDTO> toDTOPlayerPairList(List<PlayerPair> playerPairs);

  Set<PlayerPairDTO> toDTOPlayerPairSet(Set<PlayerPair> playerPairs);

  RoundDTO toDTO(Round round);

  List<RoundDTO> toDTORoundList(List<Round> rounds);

  Set<RoundDTO> toDTORoundSet(Set<Round> rounds);

  MatchFormatDTO toDTO(MatchFormat matchFormat);

  List<MatchFormatDTO> toDTOMatchFormatList(List<MatchFormat> matchFormats);

  Set<MatchFormatDTO> toDTOMatchFormatSet(Set<MatchFormat> matchFormats);

  PoolRankingDTO toDTO(PoolRanking poolRanking);

  List<PoolRankingDTO> toDTOPoolRankingList(List<PoolRanking> poolRankings);

  Set<PoolRankingDTO> toDTOPoolRankingSet(Set<PoolRanking> poolRankings);


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
}