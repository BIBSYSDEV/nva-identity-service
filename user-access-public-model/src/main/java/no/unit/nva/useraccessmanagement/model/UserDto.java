package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.isNull;
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
import no.unit.nva.useraccessmanagement.model.interfaces.Validable;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.StringUtils;

@JsonTypeName(UserDto.TYPE)
public class UserDto implements WithCopy<Builder>, JsonSerializable, Validable, Typed {

    public static final String TYPE = "User";
    public static final String MISSING_FIELD_ERROR = "Invalid User. Missing obligatory field: ";

    public static final String INVALID_USER_ERROR_MESSAGE = "Invalid user. User should have non-empty username.";
    private List<RoleDto> roles;
    private String username;
    private String institution;
    private String givenName;
    private String familyName;

    public UserDto() {
        roles = new ArrayList<>();
    }

    private UserDto(Builder builder) {
        setUsername(builder.username);
        setGivenName(builder.givenName);
        setFamilyName(builder.familyName);
        setInstitution(builder.institution);
        setRoles(builder.roles);
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

    private void setUsername(String username) {
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
        return roles;
    }

    private void setRoles(List<RoleDto> roles) {
        this.roles = roles;
    }

    @Override
    public boolean isValid() {
        return !(isNull(username) || username.isBlank());
    }

    @Override
    public InvalidInputException exceptionWhenInvalid() {
        return new InvalidInputException(INVALID_USER_ERROR_MESSAGE);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
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
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getRoles());
    }

    private List<RoleDto> listRoles() {
        return new ArrayList<>(Optional.ofNullable(roles).orElse(Collections.emptyList()));
    }

    public static final class Builder {

        private String username;
        private String givenName;
        private String familyName;
        private String institution;
        private List<RoleDto> roles;

        private Builder() {
            roles = Collections.emptyList();
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withInstitution(String institution) {
            this.institution = institution;
            return this;
        }

        public Builder withRoles(List<RoleDto> roles) {
            this.roles = roles;
            return this;
        }

        public Builder withGivenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public Builder withFamilyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        /**
         * creates a UserDto instance.
         *
         * @return a {@link UserDto}
         * @throws InvalidEntryInternalException when the used to be built is invalid.
         */
        public UserDto build() throws InvalidEntryInternalException {
            if (StringUtils.isBlank(username)) {
                throw new InvalidEntryInternalException(MISSING_FIELD_ERROR + "username");
            }
            return new UserDto(this);
        }
    }
}
