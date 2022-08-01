package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@SuppressWarnings("PMD")

public class FsCoursesSearchResult {

    @JsonProperty("items")
    final List<FsCourseData> fsCourseData;

    @JsonCreator
    public FsCoursesSearchResult(@JsonProperty("items") List<FsCourseData> fsCourseData) {
        this.fsCourseData = fsCourseData;
    }

    public List<FsCourseData> getFsCourseData() {
        return fsCourseData;
    }
}
