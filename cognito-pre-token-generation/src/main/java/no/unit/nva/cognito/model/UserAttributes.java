package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserAttributes {

    @JsonProperty("custom:feideId")
    private String feideId;

    @JsonProperty("custom:orgNumber")
    private String orgNumber;

    @JsonProperty("custom:affiliation")
    private String affiliation;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("custom:hostedOrgNumber")
    private String hostedOrgNumber;

    @JsonProperty("custom:hostedAffiliation")
    private String hostedAffiliation;

    public String getFeideId() {
        return feideId;
    }

    public void setFeideId(String feideId) {
        this.feideId = feideId;
    }

    public String getOrgNumber() {
        return orgNumber;
    }

    public void setOrgNumber(String orgNumber) {
        this.orgNumber = orgNumber;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getHostedOrgNumber() {
        return hostedOrgNumber;
    }

    public void setHostedOrgNumber(String hostedOrgNumber) {
        this.hostedOrgNumber = hostedOrgNumber;
    }

    public String getHostedAffiliation() {
        return hostedAffiliation;
    }

    public void setHostedAffiliation(String hostedAffiliation) {
        this.hostedAffiliation = hostedAffiliation;
    }
}
