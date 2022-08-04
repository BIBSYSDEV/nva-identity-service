package no.unit.identityservice.fsproxy.model.Fagperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsUriToCourseActivity {

    @JsonProperty("href")
    private String uriToCourse;

    @JsonCreator
    public FsUriToCourseActivity(@JsonProperty("href") String uriToCourse) {
        this.uriToCourse = uriToCourse;
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

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(uriToCourse);
    }

    public String getUri() {
        return uriToCourse;
    }
}
