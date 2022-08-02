package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class FsCourseData {

    @JsonProperty("id")
    private final FsCourse fsCourse;

    @JsonCreator
    public FsCourseData(@JsonProperty("id") FsCourse fsCourse) {
        this.fsCourse = fsCourse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fsCourse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FsCourseData that = (FsCourseData) o;
        return fsCourse.equals(that.fsCourse);
    }

    public FsCourse getFsCourse() {
        return fsCourse;
    }
}