package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.identityservice.json.JsonConfig;

public class CreateExternalClientRequest {

    private static final String CLIENT_NAME = "clientName";
    private static final String CUSTOMER_URI = "customerUri";
    private static final String CRISTIN_URI = "cristinUri";
    private static final String OWNER = "owner";
    private static final String SCOPES = "scopes";

    @JsonProperty(CLIENT_NAME)
    private String clientName;

    @JsonProperty(CUSTOMER_URI)
    private URI customerUri;

    @JsonProperty(CRISTIN_URI)
    private URI cristinUri;

    @JsonProperty(OWNER)
    private String owner;

    @JsonProperty(SCOPES)
    private List<String> scopes;


    public CreateExternalClientRequest() {
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setCustomerUri(URI customerUri) {
        this.customerUri = customerUri;
    }

    public void setCristinUri(URI cristinUri) {
        this.cristinUri = cristinUri;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getClientName() {
        return clientName;
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

    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }

    public static final class Builder {

        private final CreateExternalClientRequest request;

        private Builder() {
            request = new CreateExternalClientRequest();
        }

        public Builder withClientName(String clientName) {
            request.setClientName(clientName);
            return this;
        }

        public Builder withCustomerUri(URI customerUri) {
            request.setCustomerUri(customerUri);
            return this;
        }

        public Builder withCristinUri(URI cristinUri) {
            request.setCristinUri(cristinUri);
            return this;
        }

        public Builder withOwner(String owner) {
            request.setOwner(owner);
            return this;
        }

        public Builder withScopes(List<String> scopes) {
            request.setScopes(scopes);
            return this;
        }

        public CreateExternalClientRequest build() {
            return request;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }


}
