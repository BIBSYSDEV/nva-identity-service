package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsIdNumber {

    private final Integer identifier;

    @JsonCreator
    public FsIdNumber(Integer identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identifier);
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
        FsIdNumber that = (FsIdNumber) o;
        return Objects.equals(identifier, that.identifier);
    }

    @JsonValue
    @Override
    public String toString() {
        return Integer.toString(identifier);
    }
}
