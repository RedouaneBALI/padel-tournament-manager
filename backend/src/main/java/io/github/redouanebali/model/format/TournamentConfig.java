package io.github.redouanebali.model.format;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "tournamentFormat")
@JsonSubTypes({
    @JsonSubTypes.Type(value = KnockoutConfig.class, name = "KNOCKOUT"),
    @JsonSubTypes.Type(value = GroupsKoConfig.class, name = "GROUPS_KO")
})
public interface TournamentConfig {

}