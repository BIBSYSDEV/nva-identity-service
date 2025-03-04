package no.unit.nva.useraccessservice.dao;

import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleName;
import nva.commons.apigateway.AccessRight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Collections;
import java.util.Set;

import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static no.unit.nva.RandomUserDataGenerator.randomRoleNameButNot;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
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

public class RoleDbTest {

    public static final String SOME_OTHER_RANGE_KEY = "SomeOtherRangeKey";
    public static final String SOME_TYPE = "SomeType";
    public static final RoleSetConverter ROLE_SET_CONVERTER = new RoleSetConverter();
    private final RoleDb sampleRole = createSampleRole(randomRoleName().getValue());

    public RoleDbTest() throws InvalidEntryInternalException {
    }

    @Test
    public void getPrimaryHashKeyReturnsStringContainingTypeRole() {
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(RoleDb.TYPE_VALUE));
    }

    @Test
    public void objectIsStoredWithType() throws IntrospectionException {
        var role = RoleDb.newBuilder().withName(randomRoleName()).build();
        var beanInfo = Introspector.getBeanInfo(RoleDb.class);
        var item = RoleDb.TABLE_SCHEMA.itemToMap(role, true);
        assertThat(item.get(RoleDb.TYPE_FIELD), is(not(nullValue())));
    }

    @Test
    public void roleDbHasListOfAccessRights() {
        assertThat(sampleRole.getAccessRights(), is(not(nullValue())));
    }

    @Test
    void equalsComparesAllFields() throws InvalidEntryInternalException {
        RoleDb left = sampleRole;
        RoleDb right = sampleRole.copy().build();
        assertThat(sampleRole, doesNotHaveEmptyValues());
        assertThat(left, is(equalTo(right)));
    }

    @Test
    void equalsReturnsFalseWhenNameIsDifferent() throws InvalidEntryInternalException {
        RoleDb left = sampleRole;
        RoleDb right = sampleRole.copy().withName(randomRoleNameButNot(sampleRole.getName())).build();
        assertThat(left, is(not(equalTo(right))));
    }

    @Test
    void equalsReturnsFalseWhenAccessRightListIsDifferent() throws InvalidEntryInternalException {

        Set<AccessRight> differentAccessRights = Collections.singleton(AccessRight.MANAGE_DEGREE);
        assertThat(sampleRole.getAccessRights().containsAll(differentAccessRights), is(equalTo(false)));
        RoleDb differentRole = sampleRole.copy().withAccessRights(differentAccessRights).build();

        assertThat(sampleRole, is(not(equalTo(differentRole))));
    }

    @Test
    void roleDbHasBuilder() {
        RoleDb.Builder builder = RoleDb.newBuilder();
        assertNotNull(builder);
    }

    @Test
    void roleDbHasRoleName() throws InvalidEntryInternalException {
        var roleNameValue = randomRoleName().getValue();
        RoleDb roleDbEntry = createSampleRole(roleNameValue);
        assertThat(roleDbEntry.getName().getValue(), is(equalTo(roleNameValue)));
    }

    private RoleDb createSampleRole(String roleNameValue) throws InvalidEntryInternalException {
        Set<AccessRight> accessRights = Collections.singleton(AccessRight.MANAGE_DOI);
        return RoleDb.newBuilder()
            .withName(RoleName.fromValue(roleNameValue))
            .withAccessRights(accessRights)
            .build();
    }

    @Test
    void builderSetsTheRolename() throws InvalidEntryInternalException {
        var name = randomRoleName();
        RoleDb role = RoleDb.newBuilder().withName(name).build();
        assertThat(role.getName(), is(equalTo(name)));
    }

    @Test
    void buildReturnsObjectWithInitializedPrimaryHashKey() throws InvalidEntryInternalException {
        RoleDb role = RoleDb.newBuilder().withName(randomRoleName()).build();
        assertThat(role.getPrimaryKeyHashKey(), is(not(nullValue())));
        assertThat(role.getPrimaryKeyHashKey(), is(not(emptyString())));
    }

    @Test
    void buildWithoutRoleNameShouldThrowException() {
        Executable action = () -> RoleDb.newBuilder().build();
        assertThrows(InvalidEntryInternalException.class, action);
    }

    @Test
    void getPrimaryHashKeyReturnsStringContainingRoleName() {
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(sampleRole.getName().getValue()));
    }

    @Test
    void setPrimaryHashKeyShouldNotChangeTheValueOfAlreadySetPrimaryHashKey() throws InvalidEntryInternalException {
        String someOtherHashKey = "SomeOtherHashKey";
        sampleRole.setPrimaryKeyHashKey(someOtherHashKey);
        assertThat(sampleRole.getPrimaryKeyHashKey(), is(not(equalTo(someOtherHashKey))));
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(sampleRole.getName().getValue()));
        assertThat(sampleRole.getPrimaryKeyHashKey(), containsString(RoleDb.TYPE_VALUE));
    }

    @Test
    void setPrimaryRangeKeyHasNoEffect() throws InvalidEntryInternalException {
        RoleDb originalRole = RoleDb.newBuilder().withName(randomRoleName()).build();
        RoleDb copy = originalRole.copy().build();
        copy.setPrimaryKeyRangeKey(SOME_OTHER_RANGE_KEY);
        assertThat(originalRole, is(equalTo(copy)));
    }

    @Test
    void setTypeHasNoEffect() throws InvalidEntryInternalException {
        RoleDb originalRole = RoleDb.newBuilder().withName(randomRoleName()).build();
        RoleDb copy = originalRole.copy().build();
        copy.setType(SOME_TYPE);
        assertThat(originalRole, is(equalTo(copy)));
    }

    @Test
    void copyReturnsBuilderContainingAllFieldValuesOfOriginalItem() throws InvalidEntryInternalException {
        RoleDb copyRole = sampleRole.copy().build();
        assertThat(copyRole, is(equalTo(sampleRole)));
        assertThat(copyRole, is(not(sameInstance(sampleRole))));
    }

    @Test
    void converterShouldConvertRoleToAttributeValueAndBackWithoutDataLoss() {
        var expected = Set.of(sampleRole);
        assertThat(sampleRole, doesNotHaveEmptyValues());
        var attributeValue = ROLE_SET_CONVERTER.transformFrom(Set.of(sampleRole));
        var actual = ROLE_SET_CONVERTER.transformTo(attributeValue);
        assertThat(actual, is(equalTo(expected)));
    }
}