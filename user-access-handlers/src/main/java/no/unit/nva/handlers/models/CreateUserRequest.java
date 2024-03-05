package no.unit.nva.handlers.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.Set;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.ViewingScope;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
@JsonTypeInfo(use = Id.NAME)
public record CreateUserRequest(String nationalIdentityNumber, URI customerId, Set<RoleDto> roles,
                                ViewingScope viewingScope) {


}
