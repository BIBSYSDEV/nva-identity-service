package no.unit.identityservice.fsproxy.model.staffperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.identityservice.fsproxy.model.course.FsCourse;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class FsCourseActivity implements JsonSerializable {

    @JsonProperty("undervisning")
    private final FsCourse course;

    @JsonCreator
    public FsCourseActivity(@JsonProperty("undervisning") FsCourse course) {
        this.course = course;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(course);
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
        FsCourseActivity that = (FsCourseActivity) o;
        return Objects.equals(course, that.course);
    }

    @JacocoGenerated
    @JsonIgnore
    public FsCourse getCourse() {
        return course;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
