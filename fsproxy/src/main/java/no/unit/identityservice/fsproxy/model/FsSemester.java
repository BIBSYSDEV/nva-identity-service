package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD")

public class FsSemester {

    @JsonProperty("ar")
    final String year;

    @JsonProperty("termin")
    final String termin;

    @JsonCreator
    public FsSemester(@JsonProperty("ar") String year, @JsonProperty("termin") String termin) {
        this.year = year;
        this.termin = termin;
    }

    public String getYear() {
        return year;
    }

    public String getTermin() {
        return termin;
    }

}
