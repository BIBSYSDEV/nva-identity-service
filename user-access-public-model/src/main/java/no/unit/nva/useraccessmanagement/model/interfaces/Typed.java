package no.unit.nva.useraccessmanagement.model.interfaces;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = TypedObjectsDetails.TYPE_ATTRIBUTE)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserDto.class, name = "User"),
    @JsonSubTypes.Type(value = RoleDto.class, name = "Role")
})
public interface Typed {
}
