package io.github.redouanebali.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.redouanebali.model.Stage;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class RoundDTO {


  private Long           id;
  private Stage          stage;
  private List<GameDTO>  games;
  private MatchFormatDTO matchFormat;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<PoolDTO>  pools = new LinkedList<>();
}