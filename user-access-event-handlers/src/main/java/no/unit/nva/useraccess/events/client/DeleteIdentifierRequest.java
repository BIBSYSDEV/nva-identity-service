package no.unit.nva.useraccess.events.client;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

public class DeleteIdentifierRequest {

    private URI identifier;

    @JsonCreator
    public DeleteIdentifierRequest(@JsonProperty("identifier") URI identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    public URI getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    public void setIdentifier(URI identifier) {
        this.identifier = identifier;
    }

    public String toJson() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
