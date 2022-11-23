package no.unit.nva.useraccessservice.usercreation.person.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class CristinCredentials {
    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    @JsonProperty(USERNAME_FIELD)
    private final String username;
    @JsonProperty(PASSWORD_FIELD)
    private final char[] password;

    @SuppressWarnings("PMD.UseVarargs")
    @JsonCreator
    public CristinCredentials(@JsonProperty(USERNAME_FIELD) String username,
                              @JsonProperty(PASSWORD_FIELD) char[] password) {
        this.username = username;
        this.password = Arrays.copyOf(password, password.length);
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return Arrays.copyOf(password, password.length);
    }
}
