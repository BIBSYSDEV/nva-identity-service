package no.unit.nva.useraccessservice.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identityservice.json.JsonConfig;

public class CreateExternalUserResponse {

    public static final String CLIENT_ID_FIELD = "clientId";
    public static final String CLIENT_SECRET_FIELD = "clientSecret";
    public static final String CLIENT_URL_FIELD = "clientUrl";

    @JsonProperty(CLIENT_ID_FIELD)
    private String clientId;
    @JsonProperty(CLIENT_SECRET_FIELD)
    private String clientSecret;
    @JsonProperty(CLIENT_URL_FIELD)
    private String clientUrl;

    public CreateExternalUserResponse() {

    }

    public CreateExternalUserResponse(String clientId, String clientSecret, String clientUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientUrl = clientUrl;
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

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
