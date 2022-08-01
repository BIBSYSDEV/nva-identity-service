package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public class FsIdSearchResult {

    @JsonProperty("id")
    private FsPerson fsPerson;

    public FsPerson getFsPerson() {
        return fsPerson;
    }

    public void setFsPerson(FsPerson fsPerson) {
        this.fsPerson = fsPerson;
    }
}
