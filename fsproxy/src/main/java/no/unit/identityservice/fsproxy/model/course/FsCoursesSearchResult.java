package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class FsCoursesSearchResult implements JsonSerializable {
    
    @JsonProperty("items")
    private final List<FsCourseItemContainingCourseContainer> fsCourseItemContainingCourseData;
    
    @JsonCreator
    public FsCoursesSearchResult(
        @JsonProperty("items") List<FsCourseItemContainingCourseContainer> fsCourseItemContainingCourseData) {
        this.fsCourseItemContainingCourseData = fsCourseItemContainingCourseData;
    }
    
    public List<FsCourseItemContainingCourseContainer> getItems() {
        return fsCourseItemContainingCourseData;
    }
    
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
