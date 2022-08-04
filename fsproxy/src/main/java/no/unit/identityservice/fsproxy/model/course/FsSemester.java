package no.unit.identityservice.fsproxy.model.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class FsSemester {

    @JsonProperty("ar")
    private final int year;
    @JsonProperty("termin")
    private final String term;
    
    @JsonCreator
    public FsSemester(@JsonProperty("ar") int year, @JsonProperty("termin") String term) {
        this.year = year;
        this.term = term;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getYear(), getTerm());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FsSemester)) {
            return false;
        }
        FsSemester that = (FsSemester) o;
        return getYear() == that.getYear() && Objects.equals(getTerm(), that.getTerm());
    }
    
    public int getYear() {
        return year;
    }
    
    public String getTerm() {
        return term;
    }
}
