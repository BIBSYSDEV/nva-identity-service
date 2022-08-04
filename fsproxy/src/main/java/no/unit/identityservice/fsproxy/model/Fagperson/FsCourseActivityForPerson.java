package no.unit.identityservice.fsproxy.model.Fagperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.identityservice.fsproxy.model.Course.FsCourse;
import nva.commons.core.JacocoGenerated;

public class FsCourseActivityForPerson {

    @JsonProperty("undervisning")
    private final FsCourse course;

    @JsonCreator
    public FsCourseActivityForPerson(@JsonProperty("undervisning") FsCourse course) {
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
        FsCourseActivityForPerson that = (FsCourseActivityForPerson) o;
        return Objects.equals(course, that.course);
    }

    public FsCourse getCourse() {
        return course;
    }
}
