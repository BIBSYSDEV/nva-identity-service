package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsCourseContainer {

    @JsonProperty("undervisning")
    private final FsCourse course;

    @JsonCreator
    public FsCourseContainer(@JsonProperty("undervisning") FsCourse course) {
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
        FsCourseContainer fsCourseContainer = (FsCourseContainer) o;
        return course.equals(fsCourseContainer.course);
    }

    public FsCourse getCourse() {
        return course;
    }
}
