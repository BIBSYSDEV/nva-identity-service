package no.unit.nva.useraccessmanagement.dao;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.useraccessmanagement.dao.RoleDb.Builder;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.useraccessserivce.accessrights.AccessRight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class RoleDbTest {

    public static final String SOME_ROLE_NAME = "someRoleName";
    public static final String SOME_OTHER_RANGE_KEY = "SomeOtherRangeKey";
    public static final String SOME_TYPE = "SomeType";
    private final RoleDao sampleRole = createSampleRole();

    public RoleDbTest() throws InvalidEntryInternalException {
    }

    @Test
    public void getPrimaryHashKeyReturnsStringContainingTypeRole() {
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(RoleDao.TYPE));
    }

    @Test
    public void equalsComparesAllFields() throws InvalidEntryInternalException {
        RoleDao left = sampleRole;
        RoleDb right = sampleRole.copy().build();
        assertThat(sampleRole, doesNotHaveNullOrEmptyFields());
        assertThat(left, is(equalTo(right)));
    }

    @Test
    public void equalsReturnsFalseWhenNameIsDifferent() throws InvalidEntryInternalException {
        RoleDao left = sampleRole;
        RoleDao right = sampleRole.copy().withName("SomeOtherName").build();
        assertThat(left, is(not(equalTo(right))));
    }

    @Test
    public void equalsReturnsFalseWhenAccessRightListIsDifferent() throws InvalidEntryInternalException {

        Set<AccessRight> differentAccessRights = Collections.singleton(AccessRight.REJECT_DOI_REQUEST);
        assertThat(sampleRole.getAccessRights().containsAll(differentAccessRights), is(equalTo(false)));
        RoleDao differentRole = sampleRole.copy().withAccessRights(differentAccessRights).build();

        assertThat(sampleRole, is(not(equalTo(differentRole))));
    }

    @Test
    public void roleDbHasListOfAccessRights() {
        assertThat(sampleRole.getAccessRights(), is(not(nullValue())));
    }

    @Test
    void roleDbHasBuilder() {
        RoleDao.Builder builder = RoleDao.newBuilder();
        assertNotNull(builder);
    }

    @Test
    void roleDbHasRoleName() throws InvalidEntryInternalException {
        RoleDao roleDbEntry = createSampleRole();
        assertThat(roleDbEntry.getName(), is(equalTo(SOME_ROLE_NAME)));
    }

    @Test
    void builderSetsTheRolename() throws InvalidEntryInternalException {
        RoleDao role = RoleDao.newBuilder().withName(SOME_ROLE_NAME).build();
        assertThat(role.getName(), is(equalTo(SOME_ROLE_NAME)));
    }

    @Test
    void buildReturnsObjectWithInitializedPrimaryHashKey() throws InvalidEntryInternalException {
        RoleDao role = RoleDao.newBuilder().withName(SOME_ROLE_NAME).build();
        assertThat(role.getPrimaryKeyHashKey(), is(not(nullValue())));
        assertThat(role.getPrimaryKeyHashKey(), is(not(emptyString())));
    }

    @Test
    void buildWithoutRoleNameShouldThrowException() {
        Executable action = () -> RoleDao.newBuilder().build();
        assertThrows(InvalidEntryInternalException.class, action);
    }

    @Test
    void getPrimaryHashKeyReturnsStringContainingRoleName() {
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(sampleRole.getName()));
    }

    @Test
    void setPrimaryHashKeyShouldNotChangeTheValueOfAlreadySetPrimaryHashKey() throws InvalidEntryInternalException {
        String someOtherHashKey = "SomeOtherHashKey";
        sampleRole.setPrimaryKeyHashKey(someOtherHashKey);
        assertThat(sampleRole.getPrimaryKeyHashKey(), is(not(equalTo(someOtherHashKey))));
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(sampleRole.getName()));
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(RoleDao.TYPE));
    }

    @ParameterizedTest(name = "setPrimaryHashKey throws exception when input is:\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\r"})
    void setPrimaryHashKeyThrowsExceptionWhenInputIsBlankOrNullString(String blankString) {
        Executable action = () -> RoleDao.newBuilder().withName(blankString).build();
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), containsString(Builder.EMPTY_ROLE_NAME_ERROR));
    }

    @Test
    void setPrimaryRangeKeyHasNoEffect() throws InvalidEntryInternalException {
        RoleDao originalRole = RoleDao.newBuilder().withName(SOME_ROLE_NAME).build();
        RoleDao copy = originalRole.copy().build();
        copy.setPrimaryKeyRangeKey(SOME_OTHER_RANGE_KEY);
        assertThat(originalRole, is(equalTo(copy)));
    }

    @Test
    void setTypeHasNoEffect() throws InvalidEntryInternalException {
        RoleDao originalRole = RoleDao.newBuilder().withName(SOME_ROLE_NAME).build();
        RoleDao copy = originalRole.copy().build();
        copy.setType(SOME_TYPE);
        assertThat(originalRole, is(equalTo(copy)));
    }

    @Test
    void copyReturnsBuilderContainingAllFieldValuesOfOriginalItem() throws InvalidEntryInternalException {
        RoleDao copyRole = sampleRole.copy().build();
        assertThat(copyRole, is(equalTo(sampleRole)));
        assertThat(copyRole, is(not(sameInstance(sampleRole))));
    }

    private RoleDao createSampleRole() throws InvalidEntryInternalException {
        Set<AccessRight> accessRights = Collections.singleton(AccessRight.APPROVE_DOI_REQUEST);
        return RoleDao.newBuilder()
            .withName(SOME_ROLE_NAME)
            .withAccessRights(accessRights)
            .build();
    }
}