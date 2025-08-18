package io.github.redouanebali.mapper;

import io.github.redouanebali.config.SecurityProps;
import io.github.redouanebali.config.SecurityUtil;
import io.github.redouanebali.dto.TournamentDTO;
import io.github.redouanebali.model.Tournament;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapper {

  private final SecurityProps securityProps;

  public TournamentMapper(SecurityProps securityProps) {
    this.securityProps = securityProps;
  }

  public TournamentDTO toDTO(Tournament tournament) {
    TournamentDTO dto = new TournamentDTO();
    dto.setId(tournament.getId());
    dto.setOwnerId(tournament.getOwnerId());
    dto.setName(tournament.getName());
    dto.setRounds(tournament.getRounds());
    dto.setPlayerPairs(tournament.getPlayerPairs());
    dto.setDescription(tournament.getDescription());
    dto.setCity(tournament.getCity());
    dto.setClub(tournament.getClub());
    dto.setGender(tournament.getGender());
    dto.setLevel(tournament.getLevel());
    dto.setTournamentFormat(tournament.getTournamentFormat());
    dto.setNbSeeds(tournament.getNbSeeds());
    dto.setStartDate(tournament.getStartDate());
    dto.setEndDate(tournament.getEndDate());
    dto.setNbMaxPairs(tournament.getNbMaxPairs());
    dto.setNbPools(tournament.getNbPools());
    dto.setNbPairsPerPool(tournament.getNbPairsPerPool());
    dto.setNbQualifiedByPool(tournament.getNbQualifiedByPool());

    String  me       = SecurityUtil.currentUserId();
    boolean editable = false;
    if (me != null) {
      editable = securityProps.getSuperAdmins().contains(me) || me.equals(tournament.getOwnerId());
    }
    dto.setEditable(editable);

    return dto;
  }
}