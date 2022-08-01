package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD")

public class FsUndervisning {

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
