package no.unit.identityservice.fsproxy.model.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FsIdSearchResult {

    @JsonProperty("id")
    private final FsPerson fsPerson;

    @JsonCreator
    public FsIdSearchResult(@JsonProperty("id") FsPerson fsPerson) {
        this.fsPerson = fsPerson;
    }

    public FsPerson getFsPerson() {
        return fsPerson;
    }
}
