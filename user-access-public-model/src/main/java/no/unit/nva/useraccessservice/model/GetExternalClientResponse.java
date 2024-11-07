package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

import static nva.commons.core.attempt.Try.attempt;

public class GetExternalClientResponse {

    public static final String CLIENT_ID_FIELD = "clientId";
    public static final String CUSTOMER_URI_FIELD = "customerUri";
    public static final String CRISTIN_ORG_URI_FIELD = "cristinOrgUri";
    public static final String ACTING_USER_FIELD = "actingUser";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;
    @JsonProperty(CUSTOMER_URI_FIELD)
    private URI customerUri;
    @JsonProperty(CRISTIN_ORG_URI_FIELD)
    private URI cristinOrgUri;
    @JsonProperty(ACTING_USER_FIELD)
    private String actingUser;

    @JacocoGenerated
    public GetExternalClientResponse() {

    }

    public GetExternalClientResponse(String clientId, URI customerUri, URI cristinOrgUri, String actingUser) {
        this.clientId = clientId;
        this.customerUri = customerUri;
        this.cristinOrgUri = cristinOrgUri;
        this.actingUser = actingUser;
    }

    public String getClientId() {
        return clientId;
    }

    public URI getCustomerUri() {
        return customerUri;
    }

    public URI getCristinOrgUri() {
        return cristinOrgUri;
    }

    public String getActingUser() {
        return actingUser;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
