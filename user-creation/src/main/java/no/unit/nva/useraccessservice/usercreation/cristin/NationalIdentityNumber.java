package no.unit.nva.useraccessservice.usercreation.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class NationalIdentityNumber {

    private final String nin;

    @JsonCreator
    public NationalIdentityNumber(String nin) {
        this.nin = nin;
    }

    public String getNin() {
        return nin;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getNin());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NationalIdentityNumber)) {
            return false;
        }
        NationalIdentityNumber that = (NationalIdentityNumber) o;
        return Objects.equals(getNin(), that.getNin());
    }

    @JacocoGenerated
    @Override
    @JsonValue
    public String toString() {
        return getNin();
    }
}
