package no.unit.nva.useraccessmanagement.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.useraccessserivce.accessrights.AccessRight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DynamoEntryWithRangeKeyTest {

    public static final String SOME_TYPE = "SomeType";
    public static final String SOME_INVALID_KEY = "SomeInvalidKey";


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