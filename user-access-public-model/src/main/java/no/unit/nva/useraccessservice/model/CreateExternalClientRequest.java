package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.identityservice.json.JsonConfig;

public class CreateExternalClientRequest {

    private static final String CLIENT_NAME = "clientName";
    private static final String CUSTOMER = "customer";
    private static final String SCOPES = "scopes";

    @JsonProperty(CLIENT_NAME)
    private String clientName;

    @JsonProperty(CUSTOMER)
    private URI customer;

    @JsonProperty(SCOPES)
    private List<String> scopes;

    public CreateExternalClientRequest() {
    }

    public CreateExternalClientRequest(String clientName, URI customer, List<String> scopes) {
        this.clientName = clientName;
        this.customer = customer;
        this.scopes = scopes;
    }

    public String getClientName() {
        return clientName;
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
