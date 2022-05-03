package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.interfaces.Typed;
import nva.commons.core.JacocoGenerated;

public class UserList implements Typed {

    private List<UserDto> users;

    @JacocoGenerated
    public UserList() {
        users = new ArrayList<>();
    }

    private UserList(List<UserDto> users) {
        this.users = users;
    }

    /**
     * This method avoids creating a copy of the input parameter. The input parameter is the actual list that is
     * contained in the {@link UserList}. The {@link ArrayList} constuctor that accepts a {@link Collection} creates a
     * physical copy of the array that is holding the data.
     *
     * @param users The list that will be contained in the {@link UserList} object
     * @return the created {@link UserList}
     */
    public static UserList fromList(List<UserDto> users) {
        return new UserList(users);
    }

    public static UserList fromJson(String json) {
        return attempt(() -> JsonConfig.readValue(json, UserList.class)).orElseThrow();
    }



    @JacocoGenerated
    public List<UserDto> getUsers() {
        return users;
    }

    @JacocoGenerated
    public void setUsers(List<UserDto> users) {
        this.users = users;
    }

    @JsonProperty(Typed.TYPE_FIELD)
    @Override
    @JacocoGenerated
    public String getType() {
        return "UserList";
    }

    @Override
    @JacocoGenerated
    public void setType(String type) {
        Typed.super.setType(type);
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
