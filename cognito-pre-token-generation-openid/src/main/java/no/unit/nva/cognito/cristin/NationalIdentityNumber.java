package no.unit.nva.cognito.cristin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public class NationalIdentityNumber {

    private final String nin;

    @JsonCreator
    public NationalIdentityNumber(String nin) {
        this.nin = nin;
    }

    public String getNin() {
        return nin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNin());
    }

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

    @JsonValue
    @Override
    public String toString() {
        return getNin();
    }
}
