package no.unit.identityservice.fsproxy.model.fagperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FsUriToCourseActivityContainer {

    @JsonProperty("undervisningsaktivitet")
    private FsUriToCourseActivity courseUri;


    @JsonCreator
    public FsUriToCourseActivityContainer(@JsonProperty("undervisningsaktivitet")
                                          FsUriToCourseActivity courseUri) {
        this.courseUri = courseUri;
    }

    public FsUriToCourseActivity getCourseUri() {
        return courseUri;
    }
}
