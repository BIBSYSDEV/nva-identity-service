package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;


public class FsCourse {

    @JsonProperty("emne")
    private FsSubject subject;
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

    public FsSubject getSubject() {
        return subject;
    }

    public FsSemester getSemester() {
        return semester;
    }
}
