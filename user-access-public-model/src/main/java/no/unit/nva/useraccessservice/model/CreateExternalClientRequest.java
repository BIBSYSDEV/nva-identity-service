package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identityservice.json.JsonConfig;

public class CreateExternalClientRequest {

    private static final String CLIENT_NAME = "clientName";

    @JsonProperty(CLIENT_NAME)
    private String clientName;

    public CreateExternalClientRequest() {
    }

    public CreateExternalClientRequest(String clientName) {
        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
