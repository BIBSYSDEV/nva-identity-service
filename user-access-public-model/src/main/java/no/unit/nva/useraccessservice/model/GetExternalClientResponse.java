package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

public class GetExternalClientResponse {

    public static final String CLIENT_ID_FIELD = "clientId";
    public static final String CUSTOMER_URI_FIELD = "customerUri";
    public static final String CRISTIN_URI_FIELD = "cristinUri";
    public static final String OWNER_FIELD = "owner";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;
    @JsonProperty(CUSTOMER_URI_FIELD)
    private URI customerUri;
    @JsonProperty(CRISTIN_URI_FIELD)
    private URI cristinUri;
    @JsonProperty(OWNER_FIELD)
    private String owner;

    @JacocoGenerated
    public GetExternalClientResponse() {

    }

    public GetExternalClientResponse(String clientId, URI customerUri, URI cristinUri, String owner) {
        this.clientId = clientId;
        this.customerUri = customerUri;
        this.cristinUri = cristinUri;
        this.owner = owner;
    }

    public String getClientId() {
        return clientId;
    }

    public URI getCustomerUri() {
        return customerUri;
    }

    public URI getCristinUri() {
        return cristinUri;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
