package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsSemester {

    @JsonProperty("ar")
    private final String year;
    @JsonProperty("termin")
    private final String termin;

    @JsonCreator
    public FsSemester(@JsonProperty("ar") String year, @JsonProperty("termin") String termin) {
        this.year = year;
        this.termin = termin;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(year, termin);
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
        FsSemester that = (FsSemester) o;
        return year.equals(that.year) && termin.equals(that.termin);
    }

    public String getYear() {
        return year;
    }

    public String getTermin() {
        return termin;
    }
}
