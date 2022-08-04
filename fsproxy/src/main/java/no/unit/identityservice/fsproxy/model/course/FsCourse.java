package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD")

public class FsCourse {

    @JsonProperty("emne")
    final FsSubject subject;
    @JsonProperty("terminnummer")
    final int termNumber;
    @JsonProperty("semester")
    final FsSemester semester;

    @JsonCreator
    public FsCourse(@JsonProperty("emne") FsSubject subject, @JsonProperty("terminnummer") int termNumber,
                    @JsonProperty("semester") FsSemester semester) {
        this.subject = subject;
        this.termNumber = termNumber;
        this.semester = semester;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(subject, termNumber, semester);
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
        return termNumber == that.termNumber && subject.equals(that.subject) && semester.equals(that.semester);
    }

    public FsSubject getSubject() {
        return subject;
    }

    public int getTermNumber() {
        return termNumber;
    }

    public FsSemester getSemester() {
        return semester;
    }
}
