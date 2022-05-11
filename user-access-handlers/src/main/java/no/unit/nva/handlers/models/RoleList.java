package no.unit.nva.handlers.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import no.unit.nva.useraccessservice.model.RoleDto;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("RoleList")
public class RoleList {

    public static final String ROLES_FIELD = "roles";
    @JsonProperty("roles")
    private final List<RoleDto> roles;

    @JsonCreator
    public RoleList(@JsonProperty(ROLES_FIELD) List<RoleDto> roles) {
        this.roles = roles;
    }

    public List<RoleDto> getRoles() {
        return roles;
    }
}
