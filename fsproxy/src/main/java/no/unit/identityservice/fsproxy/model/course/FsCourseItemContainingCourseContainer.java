package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FsCourseItemContainingCourseContainer {

    @JsonProperty("id")
    private final FsCourseContainer fsCourseContainer;

    @JsonCreator
    public FsCourseItemContainingCourseContainer(@JsonProperty("id") FsCourseContainer fsCourseContainer) {
        this.fsCourseContainer = fsCourseContainer;
    }

    public static List<FsCourseItemContainingCourseContainer> fromCourseList(List<FsCourse> fsCourses) {
        return fsCourses.stream()
            .map(FsCourseItemContainingCourseContainer::create)
            .collect(Collectors.toList());
    }

    private static FsCourseItemContainingCourseContainer create(FsCourse course) {
        return new FsCourseItemContainingCourseContainer(new FsCourseContainer(course));
    }

    public FsCourseContainer getId() {
        return fsCourseContainer;
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
}
