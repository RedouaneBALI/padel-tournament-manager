package io.github.redouanebali.dto.response;

import io.github.redouanebali.model.Gender;
import io.github.redouanebali.model.TournamentLevel;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentSummaryDTO {

  private Long            id;
  private String          name;
  private String          city;
  private String          club;
  private TournamentLevel level;
  private Gender          gender;
  private LocalDate       startDate;
  private LocalDate       endDate;

}
