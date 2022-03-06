package no.unit.nva.useraccessmanagement.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import org.junit.jupiter.api.Test;

class UserListTest {

    @Test
    void fromUserListWrapsAListInTheUserList() throws InvalidEntryInternalException {
        UserDto user = UserDto.newBuilder().withUsername("SomeUsername").build();
        List<UserDto> list = Collections.singletonList(user);
        UserList userList = UserList.fromList(list);
        assertThat(userList.getUsers(), is(sameInstance(list)));
    }

    @Test
    void defaultConstructorCreatesNonNullEmptyList() {
        var userList = new UserList();
        assertThat(userList.getUsers(), is(not(nullValue())));
    }

    @Test
    void shouldReturnJsonAsStringRepresentation() throws IOException {
        var user = UserDto.newBuilder().withUsername("SomeUsername").build();
        var list = Collections.singletonList(user);
        var userList = UserList.fromList(list);
        var json = userList.toString();
        var deserialized = UserList.fromJson(json);
        assertThat(deserialized.getUsers(),is(not(empty())));
        assertThat(deserialized.getUsers(),containsInAnyOrder(userList.getUsers().toArray()));
    }
}