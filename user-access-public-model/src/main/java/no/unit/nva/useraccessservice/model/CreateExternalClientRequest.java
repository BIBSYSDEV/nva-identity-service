package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import no.unit.nva.identityservice.json.JsonConfig;

public class CreateExternalClientRequest {

    private static final String CLIENT_NAME = "clientName";
    private static final String CUSTOMER_URI = "customerUri";
    private static final String CRISTIN_ORG_URI = "cristinOrgUri";
    private static final String ACTING_USER = "actingUser";
    private static final String SCOPES = "scopes";

    @JsonProperty(CLIENT_NAME)
    private String clientName;

    @JsonProperty(CUSTOMER_URI)
    private URI customerUri;

    @JsonProperty(CRISTIN_ORG_URI)
    private URI cristinOrgUri;

    @JsonProperty(ACTING_USER)
    private String actingUser;

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

    public void setCristinOrgUri(URI cristinOrgUri) {
        this.cristinOrgUri = cristinOrgUri;
    }

    public void setActingUser(String actingUser) {
        this.actingUser = actingUser;
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

    public URI getCristinOrgUri() {
        return cristinOrgUri;
    }

    public String getActingUser() {
        return actingUser;
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

        public Builder withCristinOrgUri(URI cristinOrgUri) {
            request.setCristinOrgUri(cristinOrgUri);
            return this;
        }

        public Builder withActingUser(String actingUser) {
            request.setActingUser(actingUser);
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
