package no.unit.nva.useraccessmanagement.dao;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.Collections;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.useraccessserivce.accessrights.AccessRight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DynamoEntryWithRangeKeyTest {

    public static final String SOME_TYPE = "SomeType";
    public static final String SOME_INVALID_KEY = "SomeInvalidKey";
    public static final AccessRight SOME_ACCESS_RIGHT = AccessRight.APPROVE_DOI_REQUEST;
    public static final String SOME_GIVEN_NAME = "SomeGivenName";
    public static final String SOME_FAMILY_NAME = "SomeFamilyName";
    public static final String SOME_USER_NAME = "SomeUserName";
    public static final String SOME_ROLE_NAME = "SomeRole";
    public static final String SOME_INSTITUTION = "SomeInstitution";

    @Test
    public void fromItemReturnsEntryWithoutDataLoss() throws InvalidEntryInternalException {
        UserDb expectedUser = createSampleUser();
        assertThat(expectedUser, doesNotHaveNullOrEmptyFields());
        Item item = expectedUser.toItem();
        UserDb actualUser = UserDb.fromItem(item, UserDb.class);

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    public UserDb createSampleUser() throws InvalidEntryInternalException {
        RoleDb sampleRole = RoleDb.newBuilder()
            .withName(SOME_ROLE_NAME)
            .withAccessRights(Collections.singleton(SOME_ACCESS_RIGHT))
            .build();
        UserDb sampleUser = UserDb.newBuilder()
            .withUsername(SOME_USER_NAME)
            .withFamilyName(SOME_FAMILY_NAME)
            .withGivenName(SOME_GIVEN_NAME)
            .withInstitution(SOME_INSTITUTION)
            .withRoles(Collections.singletonList(sampleRole))
            .build();
        return sampleUser;
    }

    @Test
    void setTypeHasNoEffect() throws InvalidEntryInternalException {
        RoleDb roleDb = RoleDb.newBuilder().withName("SomeName").build();
        RoleDb copy = roleDb.copy().build();
        copy.setType(SOME_TYPE);

        assertThat(copy, is(equalTo(roleDb)));
    }

    @Test
    void setPrimaryRangeKeyIsNotActivatedWhenRangeKeyHasBeenSet() throws InvalidEntryInternalException {
        RoleDb roleDb = RoleDb.newBuilder().withName("SomeName").build();
        RoleDb copy = roleDb.copy().build();
        copy.setPrimaryRangeKey(SOME_INVALID_KEY);
        assertThat(copy, is(equalTo(roleDb)));
    }

    @Test
    void setPrimaryRangeKeyThrowsExceptionRangeKeyIsInvalid() {
        RoleDb roleDb = new RoleDb();
        Executable action = () -> roleDb.setPrimaryRangeKey(SOME_INVALID_KEY);
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), is(equalTo(RoleDb.INVALID_PRIMARY_RANGE_KEY)));
    }
}