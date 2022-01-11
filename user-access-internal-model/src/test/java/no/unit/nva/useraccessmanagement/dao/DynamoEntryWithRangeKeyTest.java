package no.unit.nva.useraccessmanagement.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DynamoEntryWithRangeKeyTest {

    public static final String SOME_TYPE = "SomeType";
    public static final String SOME_INVALID_KEY = "SomeInvalidKey";


    @Test
    void setTypeHasNoEffect() throws InvalidEntryInternalException {
        RoleDao roleDbEntry = RoleDao.newBuilder().withName("SomeName").build().toRoleDao();
        RoleDao copy = roleDbEntry.copy().build().toRoleDao();
        copy.setType(SOME_TYPE);

        assertThat(copy, is(equalTo(roleDbEntry)));
    }

    @Test
    void setPrimaryRangeKeyIsNotActivatedWhenRangeKeyHasBeenSet() throws InvalidEntryInternalException {
        RoleDao roleDbEntry = RoleDao.newBuilder().withName("SomeName").build();
        RoleDao copy = roleDbEntry.copy().build();
        copy.setPrimaryKeyRangeKey(SOME_INVALID_KEY);
        assertThat(copy, is(equalTo(roleDbEntry)));
    }

    @Test
    void setPrimaryRangeKeyThrowsExceptionRangeKeyIsInvalid() {
        RoleDao roleDbEntry = new RoleDao();
        Executable action = () -> roleDbEntry.setPrimaryKeyRangeKey(SOME_INVALID_KEY);
        InvalidEntryInternalException exception = assertThrows(InvalidEntryInternalException.class, action);
        assertThat(exception.getMessage(), is(equalTo(RoleDao.INVALID_PRIMARY_RANGE_KEY)));
    }
}