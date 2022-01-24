package no.unit.nva.useraccess.events.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

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
        return attempt(() -> dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
