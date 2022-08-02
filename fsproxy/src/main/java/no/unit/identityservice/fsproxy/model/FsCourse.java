package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsCourse {

    @JsonProperty("undervisning")
    private final FsUndervisning undervisning;

    @JsonCreator
    public FsCourse(@JsonProperty("undervisning") FsUndervisning undervisning) {
        this.undervisning = undervisning;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(undervisning);
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
        FsCourse fsCourse = (FsCourse) o;
        return undervisning.equals(fsCourse.undervisning);
    }

    public FsUndervisning getUndervisning() {
        return undervisning;
    }
}
