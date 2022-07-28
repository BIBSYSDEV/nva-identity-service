package no.unit.identityservice.fsproxy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class FsIdNumber {

    private final int identifier;


    @JsonCreator
    public FsIdNumber(int identifier) {
        this.identifier = identifier;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FsIdNumber that = (FsIdNumber) o;
        return Objects.equals(identifier, that.identifier);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
    @JsonValue
    @Override
    public String toString() {
        return Integer.toString(identifier);
    }
}
