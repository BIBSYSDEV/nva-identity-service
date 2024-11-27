package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.interfaces.Typed;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.UserDto.Builder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

public class UserDto implements WithCopy<Builder>, Typed {

    public static final String TYPE = "User";
    public static final String MISSING_FIELD_ERROR = "Invalid User. Missing obligatory field: ";
    public static final String USERNAME_FIELD = "username";
    public static final String VIEWING_SCOPE_FIELD = "viewingScope";
    public static final String INSTITUTION_FIELD = "institution";
    public static final String ROLES = "roles";
    public static final String AT = "@";

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
    private Set<RoleDto> roles;
    @JsonProperty("cristinId")
    private URI cristinId;
    @JsonProperty("feideIdentifier")
    private String feideIdentifier;
    @JsonAlias("insitutionCristinId")
    @JsonProperty("institutionCristinId")
    private URI institutionCristinId;
    @JsonProperty("affiliation")
    private URI affiliation;

    public UserDto() {
        roles = Collections.emptySet();
        viewingScope = ViewingScope.emptyViewingScope();
    }

    /**
     * returns a new builder.
     *
     * @return a new {@link UserDto.Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static UserDto fromJson(String input) throws BadRequestException {
        return attempt(() -> JsonConfig.readValue(input, UserDto.class))
            .orElseThrow(fail -> new BadRequestException("Could not read User:" + input, fail.getException()));
    }

    @JsonProperty("accessRights")
    public Set<AccessRight> getAccessRights() {
        return getRoles().stream()
            .flatMap(role -> role.getAccessRights().stream())
            .collect(Collectors.toSet());
    }

    public void setAccessRights(List<AccessRight> accessRights) {
        //Do nothing
    }

    public Set<RoleDto> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleDto> roles) {
        this.roles = roles;
    }

    @Override
    @JsonProperty(Typed.TYPE_FIELD)
    public String getType() {
        return UserDto.TYPE;
    }

    @Override
    public void setType(String type) throws BadRequestException {
        Typed.super.setType(type);
    }

    public Stream<String> generateRoleClaims() {
        return roles.stream()
            .map(RoleDto::getRoleName)
            .map(RoleName::getValue)
            .map(rolename -> rolename + AT + institution.toString());
    }

    @Override
    public UserDto.Builder copy() {
        return new Builder()
            .withUsername(getUsername())
            .withGivenName(getGivenName())
            .withFamilyName(getFamilyName())
            .withInstitution(getInstitution())
            .withRoles(new HashSet<>(getRoles()))
            .withViewingScope(getViewingScope())
            .withCristinId(getCristinId())
            .withInstitutionCristinId(getInstitutionCristinId())
            .withFeideIdentifier(getFeideIdentifier())
            .withAffiliation(getAffiliation());
    }

    public URI getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(URI affiliation) {
        this.affiliation = affiliation;
    }

    public String getFeideIdentifier() {
        return feideIdentifier;
    }

    public void setFeideIdentifier(String feideIdentifier) {
        this.feideIdentifier = feideIdentifier;
    }

    @JacocoGenerated
    @SuppressWarnings("PMD.NullAssignment")
    public URI getCristinId() {
        return cristinId;
    }

    @JacocoGenerated
    @SuppressWarnings("PMD.NullAssignment")
    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) throws InvalidInputException {
        if (StringUtils.isBlank(username)) {
            throw new InvalidInputException(MISSING_FIELD_ERROR + USERNAME_FIELD);
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

    public void setInstitution(URI institution) {
        this.institution = institution;
    }

    public ViewingScope getViewingScope() {
        return Optional.ofNullable(viewingScope)
            .or(this::defaultViewingScopeForUsersWithInstitution)
            .orElse(null);
    }

    public void setViewingScope(ViewingScope viewingScope) {
        this.viewingScope = viewingScope;
    }

    private Optional<ViewingScope> defaultViewingScopeForUsersWithInstitution() {
        return Optional.ofNullable(getInstitutionCristinId()).map(ViewingScope::defaultViewingScope);
    }

    public URI getInstitutionCristinId() {
        return institutionCristinId;
    }

    public void setInstitutionCristinId(URI institutionCristinId) {
        this.institutionCristinId = institutionCristinId;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getGivenName(), getFamilyName(), getViewingScope(),
            getRoles(),
            getCristinId(), getFeideIdentifier(), getInstitutionCristinId(), getAffiliation());
    }

    @Override
    @JacocoGenerated
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
            && Objects.equals(getRoles(), userDto.getRoles())
            && Objects.equals(getCristinId(), userDto.getCristinId())
            && Objects.equals(getFeideIdentifier(), userDto.getFeideIdentifier())
            && Objects.equals(getInstitutionCristinId(), userDto.getInstitutionCristinId())
            && Objects.equals(getAffiliation(), userDto.getAffiliation());
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    public static final class Builder {

        private final UserDto userDto;

        private Builder() {
            userDto = new UserDto();
        }

        public Builder withUsername(String username) {
            try {
                userDto.setUsername(username);
            } catch (InvalidInputException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder withInstitution(URI institution) {
            userDto.setInstitution(institution);
            return this;
        }

        public Builder withGivenName(String givenName) {
            userDto.setGivenName(givenName);
            return this;
        }

        public Builder withFamilyName(String familyName) {
            userDto.setFamilyName(familyName);
            return this;
        }

        public Builder withViewingScope(ViewingScope viewingScope) {
            userDto.setViewingScope(viewingScope);
            return this;
        }

        public Builder withRoles(Collection<RoleDto> roles) {
            if (nonNull(roles)) {
                userDto.setRoles(new HashSet<>(roles));
            }

            return this;
        }

        public Builder withCristinId(URI cristinId) {
            userDto.setCristinId(cristinId);
            return this;
        }

        public Builder withFeideIdentifier(String feideIdentifier) {
            userDto.setFeideIdentifier(feideIdentifier);
            return this;
        }

        public Builder withInstitutionCristinId(URI institutionCristinId) {
            userDto.setInstitutionCristinId(institutionCristinId);
            return this;
        }

        public Builder withAffiliation(URI affiliation) {
            userDto.setAffiliation(affiliation);
            return this;
        }

        public UserDto build() {
            return userDto;
        }
    }
}
