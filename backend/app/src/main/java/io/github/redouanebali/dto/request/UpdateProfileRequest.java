package io.github.redouanebali.dto.request;

import io.github.redouanebali.model.User;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(
    String name,
    String locale,
    @NotNull User.ProfileType profileType,
    String city,
    String country
) {

}
