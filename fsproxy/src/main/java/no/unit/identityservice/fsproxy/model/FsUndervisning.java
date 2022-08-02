package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD")

public class FsUndervisning {

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FsUndervisning that = (FsUndervisning) o;
        return terminNumber == that.terminNumber && emne.equals(that.emne) && semester.equals(that.semester);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(emne, terminNumber, semester);
    }

    @JsonProperty("emne")
    final FsEmne emne;

    @JsonProperty("terminnummer")
    final int terminNumber;

    @JsonProperty("semester")
    final FsSemester semester;

    @JsonCreator
    public FsUndervisning(@JsonProperty("emne") FsEmne emne,
                          @JsonProperty("terminnummer") int terminNumber,
                          @JsonProperty("semester") FsSemester semester) {
        this.emne = emne;
        this.terminNumber = terminNumber;
        this.semester = semester;
    }

    public FsEmne getEmne() {
        return emne;
    }

    public int getTerminNumber() {
        return terminNumber;
    }

    public FsSemester getSemester() {
        return semester;
    }
}
