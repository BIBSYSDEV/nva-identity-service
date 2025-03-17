package no.unit.identityservice.fsproxy.model.person;

import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class NationalIdentityNumber {

    private static final int FIRST_DIGIT_OF_PERSONAL_NUMBER = 6;
    private static final int BEGINNING = 0;
    private final String identifier;

    public NationalIdentityNumber(String identifier) {
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
        NationalIdentityNumber that = (NationalIdentityNumber) o;
        return identifier.equals(that.identifier);
    }

    public String getBirthDate() {
        return identifier.substring(BEGINNING, FIRST_DIGIT_OF_PERSONAL_NUMBER);
    }

    public String getPersonalNumber() {
        return identifier.substring(FIRST_DIGIT_OF_PERSONAL_NUMBER);
    }
}
