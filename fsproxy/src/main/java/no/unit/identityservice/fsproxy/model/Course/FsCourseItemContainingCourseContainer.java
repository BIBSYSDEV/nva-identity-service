package no.unit.identityservice.fsproxy.model.Course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsCourseItemContainingCourseContainer {

    @JsonProperty("id")
    private final FsCourseContainer fsCourseContainer;

    @JsonCreator
    public FsCourseItemContainingCourseContainer(@JsonProperty("id") FsCourseContainer fsCourseContainer) {
        this.fsCourseContainer = fsCourseContainer;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(fsCourseContainer);
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
        FsCourseItemContainingCourseContainer that = (FsCourseItemContainingCourseContainer) o;
        return fsCourseContainer.equals(that.fsCourseContainer);
    }

    public FsCourseContainer getFsCourse() {
        return fsCourseContainer;
    }
}
