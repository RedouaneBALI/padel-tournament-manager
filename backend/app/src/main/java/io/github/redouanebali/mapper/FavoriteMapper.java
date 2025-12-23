package io.github.redouanebali.mapper;

import io.github.redouanebali.dto.response.GameSummaryDTO;
import io.github.redouanebali.dto.response.TournamentSummaryDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TournamentMapper.class)
public interface FavoriteMapper {

  TournamentSummaryDTO toTournamentSummaryDTO(Tournament tournament);

  default List<TournamentSummaryDTO> toTournamentSummaryDTOList(List<Tournament> tournaments) {
    if (tournaments == null) {
      return new ArrayList<>();
    }
    return tournaments.stream().map(this::toTournamentSummaryDTO).toList();
  }

  @Mapping(target = "id", source = "id")
  @Mapping(target = "teamA", source = "teamA")
  @Mapping(target = "teamB", source = "teamB")
  @Mapping(target = "finished", expression = "java(game.isFinished())")
  @Mapping(target = "score", source = "score")
  @Mapping(target = "winnerSide", source = "winnerSide")
  @Mapping(target = "scheduledTime", source = "scheduledTime")
  @Mapping(target = "court", source = "court")
  @Mapping(target = "tournamentId", ignore = true)
  GameSummaryDTO toGameSummaryDTO(Game game);

  default List<GameSummaryDTO> toGameSummaryDTOList(List<Game> games) {
    if (games == null) {
      return new ArrayList<>();
    }
    return games.stream().map(this::toGameSummaryDTO).toList();
  }


}
