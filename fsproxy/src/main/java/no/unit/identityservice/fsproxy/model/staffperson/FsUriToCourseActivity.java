package no.unit.identityservice.fsproxy.model.staffperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

public class FsUriToCourseActivity {

    @JsonProperty("href")
    private final URI uriToCourse;

    @JsonCreator
    public FsUriToCourseActivity(@JsonProperty("href") URI uriToCourse) {
        this.uriToCourse = uriToCourse;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(uriToCourse);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FsUriToCourseActivity that = (FsUriToCourseActivity) o;
        return Objects.equals(uriToCourse, that.uriToCourse);
    }

    public URI getUri() {
        return uriToCourse;
    }
}
