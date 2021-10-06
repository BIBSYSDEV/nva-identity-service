package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.model.UserDto.Builder;
import no.unit.nva.useraccessmanagement.model.interfaces.Typed;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.StringUtils;

@JsonTypeName(UserDto.TYPE)
public class UserDto implements WithCopy<Builder>, JsonSerializable, Typed {

    public static final String TYPE = "User";
    public static final String MISSING_FIELD_ERROR = "Invalid User. Missing obligatory field: ";
    public static final String USERNAME_FIELD = "username";
    private List<RoleDto> roles;

    @JsonProperty(USERNAME_FIELD)
    private String username;
    private String institution;
    private String givenName;
    private String familyName;

    public UserDto() {
        roles = new ArrayList<>();
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
    public Set<String> getAccessRights() {
        return roles.stream()
            .flatMap(role -> role.getAccessRights().stream())
            .collect(Collectors.toSet());
    }

    public void setAccessRights(List<String> accessRights) {
        //Do nothing
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) throws InvalidInputException {
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

    public String getInstitution() {
        return institution;
    }

    private void setInstitution(String institution) {
        this.institution = institution;
    }

    public List<RoleDto> getRoles() {
        return nonNull(roles) ? roles : Collections.emptyList();
    }

    private void setRoles(List<RoleDto> roles) {
        this.roles = roles;
    }

    /**
     * Creates a copy of the object.
     *
     * @return a Builder containing the field values of the original object.
     */
    @Override
    public UserDto.Builder copy() {
        return new Builder()
            .withUsername(username)
            .withGivenName(givenName)
            .withFamilyName(familyName)
            .withInstitution(institution)
            .withRoles(listRoles());
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
               && Objects.equals(getRoles(), userDto.getRoles());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

    private List<RoleDto> listRoles() {
        return new ArrayList<>(Optional.ofNullable(roles).orElse(Collections.emptyList()));
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
            }).orElseThrow(fail->new InvalidEntryInternalException(fail.getException()));
        }

        public Builder withInstitution(String institution) {
            userDto.setInstitution(institution);
            return this;
        }

        public Builder withRoles(List<RoleDto> listRoles) {
            userDto.setRoles(listRoles);
            return this;
        }

        public UserDto build() {
            return userDto;
        }
    }
}
