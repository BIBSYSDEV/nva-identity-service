package no.unit.identityservice.fsproxy.model.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

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

    @Override
    @JsonValue
    public String toString() {
        return Integer.toString(identifier);
    }
}
