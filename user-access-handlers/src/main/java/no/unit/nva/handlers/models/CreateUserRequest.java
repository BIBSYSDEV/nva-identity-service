package no.unit.nva.handlers.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import java.util.Set;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.ViewingScope;

@JsonSerialize
public record CreateUserRequest(
    String nationalIdentityNumber,
    String cristinIdentifier,
    URI customerId,
    Set<RoleDto> roles,
    ViewingScope viewingScope
) {


}
