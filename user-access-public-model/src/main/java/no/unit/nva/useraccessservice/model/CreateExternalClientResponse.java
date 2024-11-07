package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identityservice.json.JsonConfig;

import java.net.URI;
import java.util.List;

import static nva.commons.core.attempt.Try.attempt;

public class CreateExternalClientResponse {

    public static final String CLIENT_ID_FIELD = "clientId";
    public static final String CLIENT_SECRET_FIELD = "clientSecret";
    public static final String CLIENT_URL_FIELD = "clientUrl";
    public static final String CUSTOMER_FIELD = "customer";
    public static final String SCOPES_FIELD = "scopes";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;
    @JsonProperty(CLIENT_SECRET_FIELD)
    private String clientSecret;
    @JsonProperty(CLIENT_URL_FIELD)
    private String clientUrl;
    @JsonProperty(CUSTOMER_FIELD)
    private URI customer;
    @JsonProperty(SCOPES_FIELD)
    private List<String> scopes;

    public CreateExternalClientResponse() {

    }

    public CreateExternalClientResponse(String clientId, String clientSecret, String clientUrl,
                                        URI customer, List<String> scopes) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientUrl = clientUrl;
        this.customer = customer;
        this.scopes = scopes;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public URI getCustomer() {
        return customer;
    }

    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
