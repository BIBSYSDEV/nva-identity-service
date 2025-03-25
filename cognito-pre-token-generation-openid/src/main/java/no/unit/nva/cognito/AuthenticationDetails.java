package no.unit.nva.cognito;

import nva.commons.core.JacocoGenerated;

public class AuthenticationDetails {
    private final String feideIdentifier;
    private final String feideDomain;
    private final String userPoolId;
    private final String username;
    private final String impersonating;

    public AuthenticationDetails(String feideIdentifier, String feideDomain,
                                 String userPoolId, String username, String impersonating) {
        this.feideIdentifier = feideIdentifier;
        this.feideDomain = feideDomain;
        this.userPoolId = userPoolId;
        this.username = username;
        this.impersonating = impersonating;
    }

    public String getFeideIdentifier() {
        return feideIdentifier;
    }

    public String getFeideDomain() {
        return feideDomain;
    }

    public String getUserPoolId() {
        return userPoolId;
    }

    public String getUsername() {
        return username;
    }

    public String getImpersonating() {
        return impersonating;
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "AuthenticationDetails{"
            + ", feideIdentifier='" + feideIdentifier + '\''
            + ", feideDomain='" + feideDomain + '\''
            + ", userPoolId='" + userPoolId + '\''
            + ", username='" + username + '\''
            + ", impersonating='" + impersonating + '\''
            + '}';
    }
}
