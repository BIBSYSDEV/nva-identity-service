package no.unit.identityservice.fsproxy.model.Person;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsNin {

    public static final int FIRST_DIGIT_OF_PERSONAL_NUMBER = 6;
    public static final int BEGINNING = 0;
    private final String identifier;

    public FsNin(String identifier) {
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
        FsNin that = (FsNin) o;
        return identifier.equals(that.identifier);
    }

    public String getBirthDate() {
        return identifier.substring(BEGINNING, FIRST_DIGIT_OF_PERSONAL_NUMBER);
    }

    public String getPersonalNumber() {
        return identifier.substring(FIRST_DIGIT_OF_PERSONAL_NUMBER);
    }
}
