package io.github.redouanebali.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.github.redouanebali.model.PairType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerPairDTO {

  private Long     id;
  private String   player1Name;
  private String   player2Name;
  private Integer  seed;
  @JsonInclude(Include.NON_DEFAULT)
  private boolean  bye;
  @JsonInclude(Include.NON_DEFAULT)
  private boolean  qualifierSlot;
  @JsonInclude(value = Include.CUSTOM, valueFilter = NormalTypeFilter.class)
  private PairType type;

  private String displaySeed;

  public PlayerPairDTO(String player1Name, String player2Name) {
    this.player1Name = player1Name;
    this.player2Name = player2Name;
  }

  public Integer getSeed() {
    if (bye || qualifierSlot) {
      return null;
    }
    return seed;
  }

  /**
   * Filtre personnalisé pour exclure PairType.NORMAL de la sérialisation JSON
   */
  public static class NormalTypeFilter {

    @Override
    public boolean equals(Object obj) {
      return obj == null || obj == PairType.NORMAL;
    }
  }
}
