package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FsCoursesSearchResult {

    @JsonProperty("items")
    private final List<FsCourseItemContainingCourseContainer> fsCourseItemContainingCourseData;

    @JsonCreator
    public FsCoursesSearchResult(@JsonProperty("items") List<FsCourseItemContainingCourseContainer> fsCourseItemContainingCourseData) {
        this.fsCourseItemContainingCourseData = fsCourseItemContainingCourseData;
    }

    public List<FsCourseItemContainingCourseContainer> getFsCourseData() {
        return fsCourseItemContainingCourseData;
    }
}
