package no.unit.identityservice.fsproxy.model.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.identityservice.fsproxy.model.fagperson.FsPossibleStaffPerson;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class FsPerson implements JsonSerializable {
    
    public static final String PERSONAL_NUMBER_FIELD = "personnummer0";
    public static final String BIRTH_DATE_FIELD = "fodselsdato0";
    public static final String FAGPERSON = "fagperson";
    public static final String FS_IDENTIFIER_FIELD = "personlopenummer";
    @JsonProperty(FS_IDENTIFIER_FIELD)
    private final FsIdNumber fsIdNumber;
    
    @JsonProperty(FAGPERSON)
    private final FsPossibleStaffPerson fagperson;
    
    @JsonProperty(BIRTH_DATE_FIELD)
    private final String birthDate;
    
    @JsonProperty(PERSONAL_NUMBER_FIELD)
    private final String personalNumber;
    
    @JsonCreator
    public FsPerson(@JsonProperty(FS_IDENTIFIER_FIELD) FsIdNumber fsIdNumber,
                    @JsonProperty(FAGPERSON) FsPossibleStaffPerson fagperson,
                    @JsonProperty(BIRTH_DATE_FIELD) String birthDate,
                    @JsonProperty(PERSONAL_NUMBER_FIELD) String personalNumber) {
        this.fsIdNumber = fsIdNumber;
        this.fagperson = fagperson;
        this.birthDate = birthDate;
        this.personalNumber = personalNumber;
    }
    
    public String getBirthDate() {
        return birthDate;
    }
    
    public String getPersonalNumber() {
        return personalNumber;
    }
    
    public FsIdNumber getFsIdNumber() {
        return fsIdNumber;
    }
    
    public FsPossibleStaffPerson getFagperson() {
        return fagperson;
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FsPerson)) {
            return false;
        }
        FsPerson fsPerson = (FsPerson) o;
        return Objects.equals(getFsIdNumber(), fsPerson.getFsIdNumber())
               && Objects.equals(getFagperson(), fsPerson.getFagperson())
               && Objects.equals(getBirthDate(), fsPerson.getBirthDate())
               && Objects.equals(getPersonalNumber(), fsPerson.getPersonalNumber());
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getFsIdNumber(), getFagperson(), getBirthDate(), getPersonalNumber());
    }
    
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
