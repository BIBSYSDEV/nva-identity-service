package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails;
import no.unit.nva.useraccessmanagement.dao.UserDb.Builder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.interfaces.WithCopy;
import no.unit.nva.useraccessmanagement.interfaces.WithType;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDb extends DynamoEntryWithRangeKey implements WithCopy<Builder>, WithType {

    public static final String TYPE = "USER";
    public static final String INVALID_USER_EMPTY_USERNAME = "Invalid user entry: Empty username is not allowed";
    public static final String INVALID_PRIMARY_HASH_KEY = "PrimaryHashKey of user should start with \"USER\"";
    public static final String ERROR_DUE_TO_INVALID_ROLE =
        "Failure while trying to create user with role without role-name";
    private static final String INVALID_PRIMARY_RANGE_KEY = "PrimaryRangeKey of user should start wih \"USER\"";
    private static Logger logger = LoggerFactory.getLogger(UserDb.class);

    @JsonProperty(DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY)
    private String primaryHashKey;
    @JsonProperty(DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY)
    private String primaryRangeKey;

    @JsonProperty("username")
    private String username;
    @JsonProperty("institution")
    private String institution;
    @JsonProperty("roles")
    private List<RoleDb> roles;
    @JsonProperty("givenName")
    private String givenName;
    @JsonProperty("familyName")
    private String familyName;

    public UserDb() {
        super();
    }

    private UserDb(Builder builder) throws InvalidEntryInternalException {
        super();
        setUsername(builder.username);
        setGivenName(builder.givenName);
        setFamilyName(builder.familyName);
        setInstitution(builder.institution);
        setRoles(builder.roles);
        setPrimaryHashKey(builder.primaryHashKey);
        setPrimaryRangeKey(builder.primaryRangeKey);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Transforms the DTO to a database object.
     *
     * @return a {@link UserDb}.
     * @throws InvalidEntryInternalException when the DTO contains an invalid user.
     */
    public static UserDb fromUserDto(UserDto userDto) throws InvalidEntryInternalException {
        UserDb.Builder userDb = UserDb.newBuilder()
            .withUsername(userDto.getUsername())
            .withGivenName(userDto.getGivenName())
            .withFamilyName(userDto.getFamilyName())
            .withInstitution(userDto.getInstitution())
            .withRoles(createRoleDbList(userDto));

        return userDb.build();
    }

    /**
     * Creates a {@link UserDto} from a {@link UserDb}.
     *
     * @return a data transfer object {@link UserDto}
     * @throws InvalidEntryInternalException when database object is invalid (should never happen).
     */
    public UserDto toUserDto() throws InvalidEntryInternalException {

        UserDto.Builder userDto = UserDto.newBuilder()
            .withUsername(this.getUsername())
            .withGivenName(this.getGivenName())
            .withFamilyName(this.getFamilyName())
            .withRoles(extractRoles(this))
            .withInstitution(this.getInstitution());
        return userDto.build();
    }

    @JacocoGenerated
    @Override
    public String getPrimaryHashKey() {
        return this.primaryHashKey;
    }

    /**
     * Do not use this function. This function is defined only for internal usage (by DynamoDB). The function does not
     * reset the primaryKey once it has been set. It does not throw an Exception because this method is supposed ot be
     * used only by DynamoDb. For any other purpose use the {@link UserDb.Builder}
     *
     * @param primaryHashKeyKey the primaryKey
     * @throws InvalidEntryInternalException when the primary key is invalid.
     */
    @Override
    public void setPrimaryHashKey(String primaryHashKeyKey) throws InvalidEntryInternalException {
        if (primaryHashKeyHasNotBeenSet()) {
            if (!primaryHashKeyKey.startsWith(TYPE)) {
                throw new InvalidEntryInternalException(INVALID_PRIMARY_HASH_KEY);
            }
            this.primaryHashKey = primaryHashKeyKey;
        }
    }

    @JacocoGenerated
    @Override
    public String getPrimaryRangeKey() {
        return this.primaryRangeKey;
    }

    /**
     * Do not use this function. This function is defined only for internal usage (by DynamoDB). The function does not
     * reset the primaryKey once it has been set. It does not throw an Exception because this method is supposed ot be
     * used only by DynamoDb. For any other purpose use the {@link UserDb.Builder}
     *
     * @param rangeKey the primaryRangeKey
     * @throws InvalidEntryInternalException when the primary key is invalid.
     */
    @JacocoGenerated
    @Override
    public void setPrimaryRangeKey(String rangeKey) throws InvalidEntryInternalException {
        if (primaryRangeKeyHasNotBeenSet()) {
            if (!rangeKey.startsWith(TYPE)) {
                throw new InvalidEntryInternalException(INVALID_PRIMARY_RANGE_KEY);
            }
            this.primaryRangeKey = rangeKey;
        }
    }

    @JacocoGenerated
    @JsonProperty(DatabaseIndexDetails.SECONDARY_INDEX_1_HASH_KEY)
    public String getSearchByInstitutionHashKey() {
        return this.getInstitution();
    }

    @JacocoGenerated
    public void setSearchByInstitutionHashKey(String searchByInstitutionHashKey) {

    }

    @JacocoGenerated
    @JsonProperty(DatabaseIndexDetails.SECONDARY_INDEX_1_RANGE_KEY)
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
    public List<RoleDb> getRoles() {
        return nonNull(roles) ? roles : Collections.emptyList();
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param roles the roles.
     */
    public void setRoles(List<RoleDb> roles) {
        this.roles = nonNull(roles) ? roles : Collections.emptyList();
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
            .withUsername(this.username)
            .withGivenName(this.givenName)
            .withFamilyName(this.familyName)
            .withInstitution(this.institution)
            .withRoles(this.roles);
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
        UserDb userDb = (UserDb) o;
        return Objects.equals(getPrimaryHashKey(), userDb.getPrimaryHashKey())
            && Objects.equals(getPrimaryRangeKey(), userDb.getPrimaryRangeKey())
            && Objects.equals(getUsername(), userDb.getUsername())
            && Objects.equals(getInstitution(), userDb.getInstitution())
            && Objects.equals(getRoles(), userDb.getRoles())
            && Objects.equals(getGivenName(), userDb.getGivenName())
            && Objects.equals(getFamilyName(), userDb.getFamilyName());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPrimaryHashKey(), getPrimaryRangeKey(), getUsername(), getInstitution(), getRoles(),
            getGivenName(), getFamilyName());
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

    public static final class Builder {

        private String username;
        private String givenName;
        private String familyName;
        private String institution;
        private List<RoleDb> roles;
        private String primaryHashKey;
        private String primaryRangeKey;

        private Builder() {
        }

        public Builder withUsername(String username) {
            this.username = username;
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

        public Builder withInstitution(String institution) {
            this.institution = institution;
            return this;
        }

        public Builder withRoles(Collection<RoleDb> roles) {
            this.roles = new ArrayList<>(roles);
            return this;
        }

        public UserDb build() throws InvalidEntryInternalException {
            this.primaryHashKey = formatPrimaryHashKey();
            this.primaryRangeKey = formatPrimaryRangeKey();
            return new UserDb(this);
        }

        /*For now the primary range key does not need to be different than the primary hash key*/
        private String formatPrimaryRangeKey() throws InvalidEntryInternalException {
            return formatPrimaryHashKey();
        }

        private String formatPrimaryHashKey() throws InvalidEntryInternalException {
            if (isNull(username) || username.isBlank()) {
                throw new InvalidEntryInternalException(INVALID_USER_EMPTY_USERNAME);
            } else {
                return String.join(DynamoEntryWithRangeKey.FIELD_DELIMITER, TYPE, username);
            }
        }
    }
}
