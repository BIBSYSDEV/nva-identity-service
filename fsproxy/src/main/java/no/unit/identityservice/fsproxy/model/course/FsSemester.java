package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsSemester {

    @JsonProperty("ar")
    private final String year;
    @JsonProperty("termin")
    private final String term;

    @JsonCreator
    public FsSemester(@JsonProperty("ar") String year, @JsonProperty("termin") String term) {
        this.year = year;
        this.term = term;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(year, term);
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
        return year.equals(that.year) && term.equals(that.term);
    }

    public String getYear() {
        return year;
    }

    public String getTerm() {
        return term;
    }
}
