package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;


public class FsCourse {

    @SuppressWarnings("PMD")
    @JsonProperty("emne")
    private FsSubject subject;

    @SuppressWarnings("PMD")
    @JsonProperty("semester")
    private FsSemester semester;

    @JsonCreator
    public FsCourse(@JsonProperty("emne") FsSubject subject, @JsonProperty("semester") FsSemester semester) {
        this.subject = subject;
        this.semester = semester;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(subject, semester);
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
        FsCourse that = (FsCourse) o;
        return subject.equals(that.subject) && semester.equals(that.semester);
    }

    public FsSemester getSemester() {
        return semester;
    }
}
