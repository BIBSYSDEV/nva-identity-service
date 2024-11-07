package no.unit.nva.useraccessservice.dao;

import static no.unit.nva.RandomUserDataGenerator.randomRoleName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import org.junit.jupiter.api.Test;

class DynamoEntryWithRangeKeyTest {

    public static final String SOME_TYPE = "SomeType";
    public static final String SOME_INVALID_KEY = "SomeInvalidKey";

    @Test
    void setTypeHasNoEffect() throws InvalidEntryInternalException {
        RoleDb roleDbEntry = RoleDb.newBuilder().withName(randomRoleName()).build();
        RoleDb copy = roleDbEntry.copy().build();
        copy.setType(SOME_TYPE);

        assertThat(copy, is(equalTo(roleDbEntry)));
    }

    @Test
    void setPrimaryRangeKeyIsNotActivatedWhenRangeKeyHasBeenSet() throws InvalidEntryInternalException {
        RoleDb roleDbEntry = RoleDb.newBuilder().withName(randomRoleName()).build();
        RoleDb copy = roleDbEntry.copy().build();
        copy.setPrimaryKeyRangeKey(SOME_INVALID_KEY);
        assertThat(copy, is(equalTo(roleDbEntry)));
    }
}