package no.unit.nva.useraccessmanagement.dao;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import org.junit.jupiter.api.Test;

class DynamoEntryWithRangeKeyTest {

    public static final String SOME_TYPE = "SomeType";
    public static final String SOME_INVALID_KEY = "SomeInvalidKey";

    @Test
    void setTypeHasNoEffect() throws InvalidEntryInternalException {
        RoleDb roleDbEntry = RoleDb.newBuilder().withName("SomeName").build();
        RoleDb copy = roleDbEntry.copy().build();
        copy.setType(SOME_TYPE);

        assertThat(copy, is(equalTo(roleDbEntry)));
    }

    @Test
    void setPrimaryRangeKeyIsNotActivatedWhenRangeKeyHasBeenSet() throws InvalidEntryInternalException {
        RoleDb roleDbEntry = RoleDb.newBuilder().withName("SomeName").build();
        RoleDb copy = roleDbEntry.copy().build();
        copy.setPrimaryKeyRangeKey(SOME_INVALID_KEY);
        assertThat(copy, is(equalTo(roleDbEntry)));
    }
}