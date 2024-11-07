package no.unit.nva.cognito;

import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import nva.commons.core.JacocoGenerated;

public class AuthenticationDetails {
    private final NationalIdentityNumber nin;
    private final String feideIdentifier;
    private final String feideDomain;
    private final String userPoolId;
    private final String username;


    public AuthenticationDetails(NationalIdentityNumber nin, String feideIdentifier, String feideDomain,
                                 String userPoolId, String username) {
        this.nin = nin;
        this.feideIdentifier = feideIdentifier;
        this.feideDomain = feideDomain;
        this.userPoolId = userPoolId;
        this.username = username;
    }

    public NationalIdentityNumber getNin() {
        return nin;
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

    @JacocoGenerated
    @Override
    public String toString() {
        return "AuthenticationDetails{"
                + "nin=" + nin
                + ", feideIdentifier='" + feideIdentifier + '\''
                + ", feideDomain='" + feideDomain + '\''
                + ", userPoolId='" + userPoolId + '\''
                + ", username='" + username + '\''
                + '}';
    }
}
