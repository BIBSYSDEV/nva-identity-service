package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsSubject {

    @JsonProperty("kode")
    private final String subjectCode;

    @JsonCreator
    public FsSubject(@JsonProperty("kode") String subjectCode) {
        this.subjectCode = subjectCode;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(subjectCode);
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
        FsSubject fsSubject = (FsSubject) o;
        return subjectCode.equals(fsSubject.subjectCode);
    }

    @JacocoGenerated
    @JsonIgnore
    public String getCode() {
        return subjectCode;
    }
}
