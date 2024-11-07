package no.unit.identityservice.fsproxy.model.staffperson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public class FsUriToCourseActivityContainer implements JsonSerializable {

    @JsonProperty("undervisningsaktivitet")
    private final FsUriToCourseActivity courseUri;


    @JsonCreator
    public FsUriToCourseActivityContainer(@JsonProperty("undervisningsaktivitet")
                                          FsUriToCourseActivity courseUri) {
        this.courseUri = courseUri;
    }

    public static FsUriToCourseActivityContainer fromJson(String responseBody) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(responseBody, FsUriToCourseActivityContainer.class);
    }

    @JsonIgnore
    public FsUriToCourseActivity getCourseUri() {
        return courseUri;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
