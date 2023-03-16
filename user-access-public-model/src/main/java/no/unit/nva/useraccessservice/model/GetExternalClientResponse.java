package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

public class GetExternalClientResponse {

    public static final String CLIENT_ID_FIELD = "clientId";
    public static final String CUSTOMER_FIELD = "customer";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;
    @JsonProperty(CUSTOMER_FIELD)
    private URI customer;

    @JacocoGenerated
    public GetExternalClientResponse() {

    }

    public GetExternalClientResponse(String clientId, URI customer) {
        this.clientId = clientId;
        this.customer = customer;
    }

    public String getClientId() {
        return clientId;
    }

    public URI getCustomer() {
        return customer;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
