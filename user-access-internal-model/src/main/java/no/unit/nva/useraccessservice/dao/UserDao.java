package no.unit.nva.useraccessservice.dao;

import no.unit.nva.useraccessservice.dao.UserDao.Builder;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.interfaces.WithCopy;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.apigateway.exceptions.BadRequestException;
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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.PRIMARY_KEY_RANGE_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SEARCH_USERS_BY_CRISTIN_IDENTIFIERS;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SEARCH_USERS_BY_INSTITUTION_INDEX_NAME;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SECONDARY_INDEX_1_RANGE_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SECONDARY_INDEX_2_HASH_KEY;
import static no.unit.nva.useraccessservice.constants.DatabaseIndexDetails.SECONDARY_INDEX_2_RANGE_KEY;
import static no.unit.nva.useraccessservice.dao.DynamoEntriesUtils.nonEmpty;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
@DynamoDbBean(converterProviders = {RoleSetConverterProvider.class, DefaultAttributeConverterProvider.class})
public class UserDao implements DynamoEntryWithRangeKey, WithCopy<Builder> {

    public static final TableSchema<UserDao> TABLE_SCHEMA = TableSchema.fromClass(UserDao.class);
    public static final String TYPE_VALUE = "USER";
    public static final String INVALID_USER_EMPTY_USERNAME = "Invalid user entry: Empty username is not allowed";
    public static final String ERROR_DUE_TO_INVALID_ROLE =
        "Failure while trying to create user with role without role-name";
    public static final String USERNAME_FIELD = "username";
    public static final String GIVEN_NAME_FIELD = "givenName";
    public static final String FAMILY_NAME_FIELD = "familyName";
    public static final String ROLES_LIST = "roles";
    public static final String INSTITUTION_FIELD = "institution";
    public static final String CRISTIN_ID = "cristinId";
    public static final String FEIDE_IDENTIFIER = "feideIdentifier";
    public static final String AFFILIATION_FIELD = "affiliation";
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    private String username;
    private URI institution;
    private Set<RoleDb> roles;
    private String givenName;
    private String familyName;
    private ViewingScopeDb viewingScope;
    private URI cristinId;
    private String feideIdentifier;
    private URI institutionCristinId;
    private URI affiliation;

    public UserDao() {
        super();
    }

    public static UserDao fromUserDto(UserDto userDto) {
        UserDao.Builder userDb = UserDao.newBuilder()
            .withUsername(userDto.getUsername())
            .withGivenName(userDto.getGivenName())
            .withFamilyName(userDto.getFamilyName())
            .withInstitution(userDto.getInstitution())
            .withRoles(createRoleDbSet(userDto))
            .withViewingScope(ViewingScopeDb.fromViewingScope(userDto.getViewingScope()))
            .withCristinId(userDto.getCristinId())
            .withFeideIdentifier(userDto.getFeideIdentifier())
            .withInstitutionCristinId(userDto.getInstitutionCristinId())
            .withAffiliation(userDto.getAffiliation());

        return userDb.build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private static Set<RoleDb> createRoleDbSet(UserDto userDto) {
        return userDto.getRoles().stream()
            .map(attempt(RoleDb::fromRoleDto))
            .map(attempt -> attempt.orElseThrow(UserDao::unexpectedException))
            .collect(Collectors.toSet());
    }

    /*This exception should not happen as a RoleDb should always convert to a RoleDto */
    private static <T> IllegalStateException unexpectedException(Failure<T> failure) {
        logger.error(ERROR_DUE_TO_INVALID_ROLE);
        return new IllegalStateException(failure.getException());
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
            .withViewingScope(convertViewingScope())
            .withCristinId(getCristinId())
            .withFeideIdentifier(getFeideIdentifier())
            .withInstitutionCristinId(getInstitutionCristinId())
            .withAffiliation(getAffiliation());

        return userDto.build();
    }

    private static List<RoleDto> extractRoles(UserDao userDao) {
        return Optional.ofNullable(userDao)
            .stream()
            .flatMap(user -> user.getRolesNonNull().stream())
            .map(attempt(RoleDb::toRoleDto))
            .map(attempt -> attempt.orElseThrow(UserDao::unexpectedException))
            .collect(Collectors.toList());
    }

    @DynamoDbIgnore
    public Set<RoleDb> getRolesNonNull() {
        return nonNull(roles) ? roles : Collections.emptySet();
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
    @DynamoDbAttribute(CRISTIN_ID)
    public URI getCristinId() {
        return this.cristinId;
    }

    @JacocoGenerated
    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    @DynamoDbAttribute(AFFILIATION_FIELD)
    public URI getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(URI affiliation) {
        this.affiliation = affiliation;
    }

    @DynamoDbAttribute(FEIDE_IDENTIFIER)
    public String getFeideIdentifier() {
        return this.feideIdentifier;
    }

    public void setFeideIdentifier(String feideIdentifer) {
        this.feideIdentifier = feideIdentifer;
    }

    @DynamoDbAttribute("institutionCristinId")
    public URI getInstitutionCristinId() {
        return this.institutionCristinId;
    }

    public void setInstitutionCristinId(URI institutionCristinId) {
        this.institutionCristinId = institutionCristinId;
    }

    private ViewingScope convertViewingScope() {
        return Optional.ofNullable(this.getViewingScope()).map(ViewingScopeDb::toViewingScope).orElse(null);
    }

    public ViewingScopeDb getViewingScope() {
        return viewingScope;
    }

    public void setViewingScope(ViewingScopeDb viewingScope) {
        this.viewingScope = viewingScope;
    }

    @JacocoGenerated
    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute(PRIMARY_KEY_HASH_KEY)
    public String getPrimaryKeyHashKey() {
        return primaryHashKeyIsTypeAndUsername();
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

    /*For now the primary range key does not need to be different from the primary hash key*/
    private String formatPrimaryRangeKey() {
        return primaryHashKeyIsTypeAndUsername();
    }

    private String primaryHashKeyIsTypeAndUsername() {
        checkUsername(username);
        return String.join(DynamoEntryWithRangeKey.FIELD_DELIMITER, TYPE_VALUE, username);
    }

    private void checkUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new InvalidEntryInternalException(INVALID_USER_EMPTY_USERNAME);
        }
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
    @DynamoDbSecondaryPartitionKey(indexNames = {SEARCH_USERS_BY_CRISTIN_IDENTIFIERS})
    @DynamoDbAttribute(SECONDARY_INDEX_2_HASH_KEY)
    public String getSearchByCristinIdentifiersHashKey() {
        return Optional.ofNullable(getCristinId()).map(URI::toString).orElse(null);
    }

    @JacocoGenerated
    public void setSearchByCristinIdentifiersHashKey(String searchByInstitutionHashKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @DynamoDbSecondarySortKey(indexNames = {SEARCH_USERS_BY_CRISTIN_IDENTIFIERS})
    @DynamoDbAttribute(SECONDARY_INDEX_2_RANGE_KEY)
    public String getSearchByCristinIdentifiersRangeKey() {
        return Optional.ofNullable(getInstitutionCristinId()).map(URI::toString).orElse(null);
    }

    @JacocoGenerated
    public void setSearchByCristinIdentifiersRangeKey(String searchByInstitutionRangeKey) {
        //DO NOTHING
    }

    @JacocoGenerated
    @Override
    @DynamoDbAttribute(TYPE_FIELD)
    public String getType() {
        return TYPE_VALUE;
    }

    @Override
    @JacocoGenerated
    public void setType(String type) throws BadRequestException {
        DynamoEntryWithRangeKey.super.setType(type);
    }

    @Override
    public UserDao.Builder copy() {
        return newBuilder()
            .withUsername(this.getUsername())
            .withGivenName(this.getGivenName())
            .withFamilyName(this.getFamilyName())
            .withInstitution(this.getInstitution())
            .withViewingScope(this.getViewingScope())
            .withCristinId(this.cristinId)
            .withRoles(this.getRolesNonNull())
            .withFeideIdentifier(this.getFeideIdentifier())
            .withInstitutionCristinId(this.getInstitutionCristinId())
            .withAffiliation(this.getAffiliation());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getRoles(), getGivenName(), getFamilyName(),
            getViewingScope(), getCristinId(), getFeideIdentifier(), getInstitutionCristinId(),
            getAffiliation());
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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDao userDao)) {
            return false;
        }
        return Objects.equals(getUsername(), userDao.getUsername())
            && Objects.equals(getInstitution(), userDao.getInstitution())
            && Objects.equals(getRoles(), userDao.getRoles())
            && Objects.equals(getGivenName(), userDao.getGivenName())
            && Objects.equals(getFamilyName(), userDao.getFamilyName())
            && Objects.equals(getViewingScope(), userDao.getViewingScope())
            && Objects.equals(getCristinId(), userDao.getCristinId())
            && Objects.equals(getFeideIdentifier(), userDao.getFeideIdentifier())
            && Objects.equals(getInstitutionCristinId(), userDao.getInstitutionCristinId())
            && Objects.equals(getAffiliation(), userDao.getAffiliation())
            ;
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

        public Builder withCristinId(URI cristinId) {
            userDao.setCristinId(cristinId);
            return this;
        }

        public UserDao build() {
            return userDao;
        }

        public Builder withFeideIdentifier(String feideIdentifer) {
            userDao.setFeideIdentifier(feideIdentifer);
            return this;
        }

        public Builder withInstitutionCristinId(URI insititutionCristinId) {
            userDao.setInstitutionCristinId(insititutionCristinId);
            return this;
        }

        public Builder withAffiliation(URI affiliation) {
            userDao.setAffiliation(affiliation);
            return this;
        }
    }
}
