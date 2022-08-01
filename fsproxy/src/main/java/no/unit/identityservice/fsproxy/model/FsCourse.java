package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
@SuppressWarnings("PMD")

public class FsCourse {

    @JsonProperty("undervisning")
    final FsUndervisning undervisning;

    @JsonCreator
    public FsCourse(@JsonProperty("undervisning") FsUndervisning undervisning) {
        this.undervisning = undervisning;
    }

    public FsUndervisning getUndervisning() {
        return undervisning;
    }


}
