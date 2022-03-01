package no.unit.nva.useraccessmanagement.dao;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SEARCH_USERS_BY_INSTITUTION_INDEX_NAME;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_HASH_KEY;
import static no.unit.nva.useraccessmanagement.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_RANGE_KEY;
import static no.unit.nva.useraccessmanagement.dao.DynamoEntriesUtils.nonEmpty;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.useraccessmanagement.dao.UserDao.Builder;
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
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean(converterProviders = {RoleSetConverterProvider.class, DefaultAttributeConverterProvider.class})
public class UserDao implements DynamoEntryWithRangeKey, WithCopy<Builder>, WithType {

    public static final String TYPE_VALUE = "USER";
    public static final String INVALID_USER_EMPTY_USERNAME = "Invalid user entry: Empty username is not allowed";
    public static final String ERROR_DUE_TO_INVALID_ROLE =
        "Failure while trying to create user with role without role-name";
    public static final String USERNAME_FIELD = "username";
    public static final String GIVEN_NAME_FIELD = "givenName";
    public static final String FAMILY_NAME_FIELD = "familyName";
    public static final String ROLES_LIST = "roles";
    public static final String INSTITUTION_FIELD = "institution";
    public static final String TYPE_FIELD = "type";
    public static final TableSchema<UserDao> TABLE_SCHEMA = TableSchema.fromClass(UserDao.class);

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    private String username;
    private URI institution;
    private Set<RoleDb> roles;
    private String givenName;
    private String familyName;
    private ViewingScopeDb viewingScope;

    public UserDao() {
        super();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static UserDao fromUserDto(UserDto userDto) {
        UserDao.Builder userDb = UserDao.newBuilder()
            .withUsername(userDto.getUsername())
            .withGivenName(userDto.getGivenName())
            .withFamilyName(userDto.getFamilyName())
            .withInstitution(userDto.getInstitution())
            .withRoles(createRoleDbSet(userDto))
            .withViewingScope(ViewingScopeDb.fromViewingScope(userDto.getViewingScope()));

        return userDb.build();
    }

    public ViewingScopeDb getViewingScope() {
        return viewingScope;
    }

    public void setViewingScope(ViewingScopeDb viewingScope) {
        this.viewingScope = viewingScope;
    }

    /**
     * Creates a {@link UserDto} from a {@link UserDao}.
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
            .withViewingScope(convertViewingScope());
        return userDto.build();
    }

    @JacocoGenerated
    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryKeyHashKey() {
        return formatPrimaryHashKey();
    }

    @Override
    public void setPrimaryKeyHashKey(String primaryRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @Override
    @DynamoDbSortKey
    @DynamoDbAttribute(PRIMARY_KEY_RANGE_KEY)
    public String getPrimaryKeyRangeKey() {
        return formatPrimaryRangeKey();
    }

    @Override
    @JacocoGenerated
    public void setPrimaryKeyRangeKey(String primaryRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @DynamoDbSecondaryPartitionKey(indexNames = {SEARCH_USERS_BY_INSTITUTION_INDEX_NAME})
    @DynamoDbAttribute(SECONDARY_INDEX_1_HASH_KEY)
    public String getSearchByInstitutionHashKey() {
        return nonNull(this.getInstitution()) ? this.getInstitution().toString() : null;
    }

    @JacocoGenerated
    public void setSearchByInstitutionHashKey(String searchByInstitutionHashKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @DynamoDbSecondarySortKey(indexNames = {SEARCH_USERS_BY_INSTITUTION_INDEX_NAME})
    @DynamoDbAttribute(SECONDARY_INDEX_1_RANGE_KEY)
    public String getSearchByInstitutionRangeKey() {
        return this.getUsername();
    }

    @JacocoGenerated
    public void setSearchByInstitutionRangeKey(String searchByInstitutionRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @DynamoDbAttribute(USERNAME_FIELD)
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
    @DynamoDbAttribute(GIVEN_NAME_FIELD)
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
    @DynamoDbAttribute(FAMILY_NAME_FIELD)
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

    @DynamoDbAttribute(ROLES_LIST)
    @DynamoDbIgnoreNulls
    public Set<RoleDb> getRoles() {
        return nonEmpty(roles) ? roles : null;
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param roles the roles.
     */
    @SuppressWarnings("PMD.NullAssignment")
    public void setRoles(Set<RoleDb> roles) {
        this.roles = nonEmpty(roles) ? roles : null;
    }

    @DynamoDbIgnore
    public Set<RoleDb> getRolesNonNull() {
        return nonNull(roles) ? roles : Collections.emptySet();
    }

    @JacocoGenerated
    @DynamoDbAttribute(INSTITUTION_FIELD)
    public URI getInstitution() {
        return institution;
    }

    /**
     * Method to be used only by DynamoDb mapper. Do not use. Use the builder instead.
     *
     * @param institution the institution.
     */
    public void setInstitution(URI institution) {
        this.institution = institution;
    }

    @JacocoGenerated
    @Override
    @DynamoDbAttribute(TYPE_FIELD)
    public String getType() {
        return TYPE_VALUE;
    }

    @Override
    public void setType(String ignored) {
        //DO nothing
    }

    @Override
    public UserDao.Builder copy() {
        return newBuilder()
            .withUsername(this.getUsername())
            .withGivenName(this.getGivenName())
            .withFamilyName(this.getFamilyName())
            .withInstitution(this.getInstitution())
            .withViewingScope(this.getViewingScope())
            .withRoles(this.getRolesNonNull());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getRolesNonNull(), getGivenName(), getFamilyName(),
                            getViewingScope());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDao)) {
            return false;
        }
        UserDao userDao = (UserDao) o;
        return Objects.equals(getUsername(), userDao.getUsername())
               && Objects.equals(getInstitution(), userDao.getInstitution())
               && Objects.equals(getRolesNonNull(), userDao.getRolesNonNull())
               && Objects.equals(getGivenName(), userDao.getGivenName())
               && Objects.equals(getFamilyName(), userDao.getFamilyName())
               && Objects.equals(getViewingScope(), userDao.getViewingScope());
    }

    private static Set<RoleDb> createRoleDbSet(UserDto userDto) {
        return userDto.getRoles().stream()
            .map(attempt(RoleDb::fromRoleDto))
            .map(attempt -> attempt.orElseThrow(UserDao::unexpectedException))
            .collect(Collectors.toSet());
    }

    private static List<RoleDto> extractRoles(UserDao userDao) {
        return Optional.ofNullable(userDao)
            .stream()
            .flatMap(user -> user.getRolesNonNull().stream())
            .map(attempt(RoleDb::toRoleDto))
            .map(attempt -> attempt.orElseThrow(UserDao::unexpectedException))
            .collect(Collectors.toList());
    }

    /*This exception should not happen as a RoleDb should always convert to a RoleDto */
    private static <T> IllegalStateException unexpectedException(Failure<T> failure) {
        logger.error(ERROR_DUE_TO_INVALID_ROLE);
        return new IllegalStateException(failure.getException());
    }

    private ViewingScope convertViewingScope() {
        return Optional.ofNullable(this.getViewingScope()).map(ViewingScopeDb::toViewingScope).orElse(null);
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
        return String.join(DynamoEntryWithRangeKey.FIELD_DELIMITER, TYPE_VALUE, username);
    }

    public static final class Builder {

        private final UserDao userDao;

        private Builder() {
            this.userDao = new UserDao();
        }

        public Builder withUsername(String username) {
            userDao.setUsername(username);
            return this;
        }

        public Builder withGivenName(String givenName) {
            userDao.setGivenName(givenName);
            return this;
        }

        public Builder withFamilyName(String familyName) {
            userDao.setFamilyName(familyName);
            return this;
        }

        public Builder withInstitution(URI institution) {
            userDao.setInstitution(institution);
            return this;
        }

        public Builder withRoles(Collection<RoleDb> roles) {
            userDao.setRoles(new HashSet<>(roles));
            return this;
        }

        public Builder withViewingScope(ViewingScopeDb viewingScope) {
            userDao.setViewingScope(viewingScope);
            return this;
        }

        public UserDao build() {
            return userDao;
        }
    }
}
