package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FsCourse {

    @JsonProperty("undervisning")
    private final FsUndervisning undervisning;

    @JsonCreator
    public FsCourse(@JsonProperty("undervisning") FsUndervisning undervisning) {
        this.undervisning = undervisning;
    }

    public FsUndervisning getUndervisning() {
        return undervisning;
    }
}
