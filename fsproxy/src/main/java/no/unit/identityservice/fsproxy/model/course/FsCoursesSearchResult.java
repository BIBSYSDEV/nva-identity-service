package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

import java.util.List;

public class FsCoursesSearchResult implements JsonSerializable {

    @JsonProperty("items")
    private final List<FsCourseItemContainingCourseContainer> fsCourseItemContainingCourseData;

    @JsonCreator
    public FsCoursesSearchResult(
            @JsonProperty("items") List<FsCourseItemContainingCourseContainer> fsCourseItemContainingCourseData) {
        this.fsCourseItemContainingCourseData = fsCourseItemContainingCourseData;
    }

    public static FsCoursesSearchResult fromJson(String responseBody) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(responseBody, FsCoursesSearchResult.class);
    }

    public List<FsCourseItemContainingCourseContainer> getItems() {
        return fsCourseItemContainingCourseData;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
