package io.github.redouanebali.dto.request;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class InitializeDrawRequest {

  private final List<RoundRequest> rounds = new ArrayList<>();
}

