package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class CustomerResponse {

    private final URI customerId;
    private final String cristinId;

    @JsonCreator
    public CustomerResponse(
        @JsonProperty("customerId") URI customerId,
        @JsonProperty("cristinId") String cristinId) {
        this.customerId = customerId;
        this.cristinId = cristinId;
    }

    public URI getCustomerId() {
        return customerId;
    }

    public String getCristinId() {
        return cristinId;
    }

}
