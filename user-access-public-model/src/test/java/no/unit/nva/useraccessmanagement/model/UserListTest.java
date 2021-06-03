package no.unit.nva.useraccessmanagement.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import java.util.Collections;
import java.util.List;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import org.junit.jupiter.api.Test;

public class UserListTest {

    @Test
    public void fromUserListWrapsAListInTheUserList() throws InvalidEntryInternalException {
        UserDto user = UserDto.newBuilder().withUsername("SomeUsername").build();
        List<UserDto> list = Collections.singletonList(user);
        UserList userList = UserList.fromList(list);
        assertThat(userList.getWrappedList(), is(sameInstance(list)));
    }

    @Test
    public void defaultConstructorCreatesNonNullEmptyList() {
        var userList = new UserList();
        assertThat(userList.getWrappedList(), is(not(nullValue())));
    }
}