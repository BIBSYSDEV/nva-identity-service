package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_RANGE_KEY;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.dao.UserDb.Builder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.nva.useraccessmanagement.model.ViewingScope;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDb extends DynamoEntryWithRangeKey implements WithCopy<Builder>, WithType {

    public static final String TYPE = "USER";
    public static final String INVALID_USER_EMPTY_USERNAME = "Invalid user entry: Empty username is not allowed";
    public static final String ERROR_DUE_TO_INVALID_ROLE =
        "Failure while trying to create user with role without role-name";

    private static Logger logger = LoggerFactory.getLogger(UserDb.class);

    @JsonProperty("username")
    private String username;
    @JsonProperty("institution")
    private String institution;
    @JsonProperty("roles")
    private Set<RoleDb> roles;
    @JsonProperty("givenName")
    private String givenName;
    @JsonProperty("familyName")
    private String familyName;
    @JsonProperty("viewingScope")
    private ViewingScope viewingScope;

    public UserDb() {
        super();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static UserDb fromUserDto(UserDto userDto) {
        UserDb.Builder userDb = UserDb.newBuilder()
            .withUsername(userDto.getUsername())
            .withGivenName(userDto.getGivenName())
            .withFamilyName(userDto.getFamilyName())
            .withInstitution(userDto.getInstitution())
            .withRoles(createRoleDbList(userDto))
            .withViewingScope(userDto.getViewingScope());

        return userDb.build();
    }

    public ViewingScope getViewingScope() {
        return viewingScope;
    }

    public void setViewingScope(ViewingScope viewingScope) {
        this.viewingScope = viewingScope;
    }

    /**
     * Creates a {@link UserDto} from a {@link UserDb}.
     *
     * @return a data transfer object {@link UserDto}
     */
    public UserDto toUserDto() {

        UserDto.Builder userDto = UserDto.newBuilder()
            .withUsername(this.getUsername())
            .withGivenName(this.getGivenName())
            .withFamilyName(this.getFamilyName())
            .withRoles(extractRoles(this))
            .withInstitution(this.getInstitution())
            .withViewingScope(this.getViewingScope());
        return userDto.build();
    }

    @JacocoGenerated
    @Override
    @JsonProperty(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryHashKey() {
        return formatPrimaryHashKey();
    }

    /**
     * Do not use this function. This function is defined only for internal usage (by DynamoDB). The function does not
     * reset the primaryKey once it has been set. It does not throw an Exception because this method is supposed ot be
     * used only by DynamoDb. For any other purpose use the {@link UserDb.Builder}
     *
     * @param primaryHashKeyKey the primaryKey
     */
    @Override
    public void setPrimaryHashKey(String primaryHashKeyKey) {
        // DO NOTHING
    }

    @JsonProperty(PRIMARY_KEY_RANGE_KEY)
    @JacocoGenerated
    @Override
    public String getPrimaryRangeKey() {
        return formatPrimaryRangeKey();
    }

    /**
     * Do not use this function. This function is defined only for internal usage (by DynamoDB). The function does not
     * reset the primaryKey once it has been set. It does not throw an Exception because this method is supposed ot be
     * used only by DynamoDb. For any other purpose use the {@link UserDb.Builder}
     *
     * @param rangeKey the primaryRangeKey
     */
    @JacocoGenerated
    @Override
    public void setPrimaryRangeKey(String rangeKey) {
        // do nothing
    }

    @JacocoGenerated
    @JsonProperty(SECONDARY_INDEX_1_HASH_KEY)
    public String getSearchByInstitutionHashKey() {
        return this.getInstitution();
    }

    @JacocoGenerated
    public void setSearchByInstitutionHashKey(String searchByInstitutionHashKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @JsonProperty(SECONDARY_INDEX_1_RANGE_KEY)
    public String getSearchByInstitutionRangeKey() {
        return this.getUsername();
    }

    @JacocoGenerated
    public void setSearchByInstitutionRangeKey(String searchByInstitutionRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    public String getUsername() {
        return username;
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param username the username of the user.
     */
    public void setUsername(String username) {
        checkUsername(username);
        this.username = username;
    }

    @JacocoGenerated
    public String getGivenName() {
        return givenName;
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param givenName the givenName of the user.
     */
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    @JacocoGenerated
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param familyName the familyName of the user.
     */
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    @JacocoGenerated
    public Set<RoleDb> getRoles() {
        return nonNull(roles) ? roles : Collections.emptySet();
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param roles the roles.
     */
    public void setRoles(Collection<RoleDb> roles) {
        this.roles = nonNull(roles) ? new HashSet<>(roles) : Collections.emptySet();
    }

    @JacocoGenerated
    public String getInstitution() {
        return institution;
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param institution the institution.
     */
    public void setInstitution(String institution) {
        this.institution = institution;
    }

    @JacocoGenerated
    @JsonProperty("type")
    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public UserDb.Builder copy() {
        return newBuilder()
            .withUsername(this.getUsername())
            .withGivenName(this.getGivenName())
            .withFamilyName(this.getFamilyName())
            .withInstitution(this.getInstitution())
            .withViewingScope(this.getViewingScope())
            .withRoles(this.getRoles());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDb)) {
            return false;
        }
        UserDb userDb = (UserDb) o;
        return Objects.equals(getUsername(), userDb.getUsername())
               && Objects.equals(getInstitution(), userDb.getInstitution())
               && Objects.equals(getRoles(), userDb.getRoles())
               && Objects.equals(getGivenName(), userDb.getGivenName())
               && Objects.equals(getFamilyName(), userDb.getFamilyName())
               && Objects.equals(getViewingScope(), userDb.getViewingScope());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getRoles(), getGivenName(), getFamilyName(),
                            getViewingScope());
    }

    private static Collection<RoleDb> createRoleDbList(UserDto userDto) {
        return userDto.getRoles().stream()
            .map(attempt(RoleDb::fromRoleDto))
            .map(attempt -> attempt.orElseThrow(UserDb::unexpectedException))
            .collect(Collectors.toSet());
    }

    private static List<RoleDto> extractRoles(UserDb userDb) {
        return Optional.ofNullable(userDb)
            .stream()
            .flatMap(user -> user.getRoles().stream())
            .map(attempt(RoleDb::toRoleDto))
            .map(attempt -> attempt.orElseThrow(UserDb::unexpectedException))
            .collect(Collectors.toList());
    }

    /*This exception should not happen as a RoleDb should always convert to a RoleDto */
    private static <T> IllegalStateException unexpectedException(Failure<T> failure) {
        logger.error(ERROR_DUE_TO_INVALID_ROLE);
        return new IllegalStateException(failure.getException());
    }

    private void checkUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new InvalidEntryInternalException(INVALID_USER_EMPTY_USERNAME);
        }
    }

    /*For now the primary range key does not need to be different from the primary hash key*/
    private String formatPrimaryRangeKey() {
        return formatPrimaryHashKey();
    }

    private String formatPrimaryHashKey() {
        checkUsername(username);
        return String.join(DynamoEntryWithRangeKey.FIELD_DELIMITER, TYPE, username);
    }

    public static final class Builder {

        private final UserDb userDb;

        private Builder() {
            this.userDb = new UserDb();
        }

        public Builder withUsername(String username) {
            userDb.setUsername(username);
            return this;
        }

        public Builder withGivenName(String givenName) {
            userDb.setGivenName(givenName);
            return this;
        }

        public Builder withFamilyName(String familyName) {
            userDb.setFamilyName(familyName);
            return this;
        }

        public Builder withInstitution(String institution) {
            userDb.setInstitution(institution);
            return this;
        }

        public Builder withRoles(Collection<RoleDb> roles) {
            userDb.setRoles(roles);
            return this;
        }

        public Builder withViewingScope(ViewingScope viewingScope) {
            userDb.setViewingScope(viewingScope);
            return this;
        }

        public UserDb build() {
            return userDb;
        }
    }
}
