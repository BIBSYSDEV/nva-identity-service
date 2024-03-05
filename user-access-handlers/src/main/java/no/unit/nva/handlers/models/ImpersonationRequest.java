package no.unit.nva.handlers.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

public class ImpersonationRequest {

    protected static final String NATIONAL_IDENTITY_NUMBER_FIELD = "nationalIdentityNumber";

    @JsonProperty(NATIONAL_IDENTITY_NUMBER_FIELD)
    @JsonAlias("nationalIdentityNumber")
    private String nin;

    public ImpersonationRequest(String nin) {
        this.nin = nin;
    }

    @JacocoGenerated
    public ImpersonationRequest() {

    }

    @JacocoGenerated
    public String getNin() {
        return nin;
    }

    @JacocoGenerated
    public void setNin(String nin) {
        this.nin = nin;
    }
}
