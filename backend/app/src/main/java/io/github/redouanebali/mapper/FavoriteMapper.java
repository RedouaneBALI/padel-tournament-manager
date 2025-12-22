package io.github.redouanebali.mapper;

import io.github.redouanebali.dto.response.GameSummaryDTO;
import io.github.redouanebali.dto.response.TournamentSummaryDTO;
import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

  TournamentSummaryDTO toTournamentSummaryDTO(Tournament tournament);

  default List<TournamentSummaryDTO> toTournamentSummaryDTOList(List<Tournament> tournaments) {
    if (tournaments == null) {
      return new ArrayList<>();
    }
    return tournaments.stream().map(this::toTournamentSummaryDTO).toList();
  }

  @Mapping(target = "finished", expression = "java(game.isFinished())")
  GameSummaryDTO toGameSummaryDTO(Game game);

  default List<GameSummaryDTO> toGameSummaryDTOList(List<Game> games) {
    if (games == null) {
      return new ArrayList<>();
    }
    return games.stream().map(this::toGameSummaryDTO).toList();
  }

}

