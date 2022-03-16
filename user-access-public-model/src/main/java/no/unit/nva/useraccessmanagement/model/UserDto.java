package no.unit.nva.useraccessmanagement.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidInputException;
import no.unit.nva.useraccessmanagement.interfaces.JacksonJrDoesNotSupportSets;
import no.unit.nva.useraccessmanagement.interfaces.Typed;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.model.UserDto.Builder;
import nva.commons.apigatewayv2.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public class UserDto implements WithCopy<Builder>, Typed {

    public static final String TYPE = "User";
    public static final String MISSING_FIELD_ERROR = "Invalid User. Missing obligatory field: ";
    public static final String USERNAME_FIELD = "username";
    public static final String VIEWING_SCOPE_FIELD = "viewingScope";
    public static final String INSTITUTION_FIELD = "institution";
    public static final String ROLES = "roles";

    @JsonProperty(USERNAME_FIELD)
    private String username;
    @JsonProperty(INSTITUTION_FIELD)
    private URI institution;
    @JsonProperty("givenName")
    private String givenName;
    @JsonProperty("familyName")
    private String familyName;
    @JsonProperty(VIEWING_SCOPE_FIELD)
    private ViewingScope viewingScope;
    @JsonProperty(ROLES)
    private List<RoleDto> roles;
    @JsonProperty("cristinId")
    private URI cristinId;

    public UserDto() {
        roles = Collections.emptyList();
    }

    /**
     * returns a new builder.
     *
     * @return a new {@link UserDto.Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static UserDto fromJson(String input) {
        return attempt(() -> objectMapper.beanFrom(UserDto.class, input))
            .orElseThrow(fail -> new BadRequestException("Could not read User:" + input, fail.getException()));
    }

    @JacocoGenerated
    @SuppressWarnings("PMD.NullAssignment")
    public URI getCristinId() {
        return cristinId;
    }

    @JacocoGenerated
    @SuppressWarnings("PMD.NullAssignment")
    public void setCristinId(URI cristinId) {
        this.cristinId =cristinId;
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
        return roles;
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
            .withUsername(getUsername())
            .withGivenName(getGivenName())
            .withFamilyName(getFamilyName())
            .withInstitution(getInstitution())
            .withRoles(getRoles())
            .withViewingScope(getViewingScope())
            .withCristinId(getCristinId());
    }

    public ViewingScope getViewingScope() {
        return this.viewingScope;
    }

    public void setViewingScope(ViewingScope viewingScope) {
        this.viewingScope = viewingScope;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getUsername(),
                            getInstitution(),
                            getGivenName(),
                            getFamilyName(),
                            getViewingScope(),
                            JacksonJrDoesNotSupportSets.toSet(getRoles()),
                            getCristinId());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDto)) {
            return false;
        }
        UserDto userDto = (UserDto) o;
        return Objects.equals(getUsername(), userDto.getUsername())
               && Objects.equals(getInstitution(), userDto.getInstitution())
               && Objects.equals(getGivenName(), userDto.getGivenName())
               && Objects.equals(getFamilyName(), userDto.getFamilyName())
               && Objects.equals(getViewingScope(), userDto.getViewingScope())
               && compareRolesAsSets(userDto)
               && Objects.equals(getCristinId(), userDto.getCristinId());
    }

    @Override
    public String toString() {
        return attempt(() -> objectMapper.asString(this)).orElseThrow();
    }

    private boolean compareRolesAsSets(UserDto userDto) {
        return Objects.equals(
            JacksonJrDoesNotSupportSets.toSet(getRoles()),
            JacksonJrDoesNotSupportSets.toSet(userDto.getRoles())
        );
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

        public Builder withCristinId(URI cristinIdentifier) {
            userDto.setCristinId(cristinIdentifier);
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
