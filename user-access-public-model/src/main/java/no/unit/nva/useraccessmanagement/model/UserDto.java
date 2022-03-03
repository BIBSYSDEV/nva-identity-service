package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.interfaces.JacksonJrDoesNotSupportSets;
import no.unit.nva.useraccessmanagement.interfaces.Typed;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.model.UserDto.Builder;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class UserDto implements WithCopy<Builder>, Typed {

    public static final String TYPE = "User";
    public static final String MISSING_FIELD_ERROR = "Invalid User. Missing obligatory field: ";
    public static final String USERNAME_FIELD = "username";
    public static final String VIEWING_SCOPE_FIELD = "viewingScope";
    private Set<RoleDto> roles;

    @JsonProperty(USERNAME_FIELD)
    private String username;
    private URI institution;
    private String givenName;
    private String familyName;
    @JsonProperty(VIEWING_SCOPE_FIELD)
    private ViewingScope viewingScope;

    public UserDto() {
        roles = Collections.emptySet();
    }

    /**
     * returns a new builder.
     *
     * @return a new {@link UserDto.Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @JsonProperty("accessRights")
    public List<String> getAccessRights() {
        return getRoles().stream()
            .flatMap(role -> role.getAccessRights().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    public void setAccessRights(List<String> accessRights) {
        //Do nothing
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new InvalidInputException(MISSING_FIELD_ERROR + "username");
        }
        this.username = username;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public URI getInstitution() {
        return institution;
    }

    private void setInstitution(URI institution) {
        this.institution = institution;
    }

    @Override
    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return UserDto.TYPE;
    }

    @Override
    public void setType(String type) {
        Typed.super.setType(type);
    }

    public List<RoleDto> getRoles() {
        return JacksonJrDoesNotSupportSets.toList(roles);
    }

    private void setRoles(List<RoleDto> roles) {
        this.roles = JacksonJrDoesNotSupportSets.toSet(roles);
    }

    /**
     * Creates a copy of the object.
     *
     * @return a Builder containing the field values of the original object.
     */
    @Override
    public UserDto.Builder copy() {
        return new Builder()
            .withUsername(getUsername())
            .withGivenName(getGivenName())
            .withFamilyName(getFamilyName())
            .withInstitution(getInstitution())
            .withRoles(getRoles())
            .withViewingScope(getViewingScope());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getRoles());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDto userDto = (UserDto) o;
        return Objects.equals(getUsername(), userDto.getUsername())
               && Objects.equals(getGivenName(), userDto.getGivenName())
               && Objects.equals(getFamilyName(), userDto.getFamilyName())
               && Objects.equals(getInstitution(), userDto.getInstitution())
               && Objects.equals(getViewingScope(), userDto.getViewingScope())
               && Objects.equals(getRoles(), userDto.getRoles());
    }

    @Override
    public String toString() {
        return attempt(() -> JSON.std.asString(this)).orElseThrow();
    }

    public ViewingScope getViewingScope() {
        return this.viewingScope;
    }

    public void setViewingScope(ViewingScope viewingScope) {
        this.viewingScope = viewingScope;
    }

    public static final class Builder {

        private final UserDto userDto;

        private Builder() {
            userDto = new UserDto();
        }

        public Builder withGivenName(String givenName) {
            userDto.setGivenName(givenName);
            return this;
        }

        public Builder withFamilyName(String familyName) {
            userDto.setFamilyName(familyName);
            return this;
        }

        public Builder withUsername(String username) {
            return attempt(() -> {
                userDto.setUsername(username);
                return this;
            }).orElseThrow(fail -> new InvalidEntryInternalException(fail.getException()));
        }

        public Builder withInstitution(URI institution) {
            userDto.setInstitution(institution);
            return this;
        }

        public Builder withRoles(Collection<RoleDto> listRoles) {
            if (nonNull(listRoles)) {
                userDto.setRoles(new ArrayList<>(listRoles));
            }

            return this;
        }

        public UserDto build() {
            return userDto;
        }

        public Builder withViewingScope(ViewingScope viewingScope) {
            userDto.setViewingScope(viewingScope);
            return this;
        }
    }
}
