package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FsCourseData {

    @JsonProperty("id")
    private final FsCourse fsCourse;

    @JsonCreator
    public FsCourseData(@JsonProperty("id") FsCourse fsCourse) {
        this.fsCourse = fsCourse;
    }

    public FsCourse getFsCourse() {
        return fsCourse;
    }
}
