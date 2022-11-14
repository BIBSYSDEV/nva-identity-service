package no.unit.nva.useraccessservice.usercreation.person.cristin;

public class CristinCredentials {
    private final String username;
    private final String password;

    public CristinCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
