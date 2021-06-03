package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerResponse {

    private final String customerId;
    private final String cristinId;

    @JsonCreator
    public CustomerResponse(
        @JsonProperty("customerId") String customerId,
        @JsonProperty("cristinId") String cristinId) {
        this.customerId = customerId;
        this.cristinId = cristinId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCristinId() {
        return cristinId;
    }

}
