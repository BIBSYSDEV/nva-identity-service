package no.unit.identityservice.fsproxy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public String getYear() {
        return year;
    }

    public String getTermin() {
        return termin;
    }

}
