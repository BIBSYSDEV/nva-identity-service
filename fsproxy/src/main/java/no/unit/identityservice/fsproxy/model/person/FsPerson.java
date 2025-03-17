package no.unit.identityservice.fsproxy.model.person;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class FsPerson implements JsonSerializable {

    public static final String PERSONAL_NUMBER_FIELD = "personnummer0";
    public static final String BIRTH_DATE_FIELD = "fodselsdato0";
    public static final String FAGPERSON = "fagperson";
    public static final String FS_IDENTIFIER_FIELD = "personlopenummer";
    @JsonProperty(FS_IDENTIFIER_FIELD)
    private final FsIdNumber fsIdNumber;
    @JsonProperty(FAGPERSON)
    private final String fagperson;
    @JsonProperty(BIRTH_DATE_FIELD)
    private final String birthDate;
    @JsonProperty(PERSONAL_NUMBER_FIELD)
    private final String personalNumber;

    @JsonCreator
    public FsPerson(@JsonProperty(FS_IDENTIFIER_FIELD) FsIdNumber fsIdNumber, @JsonProperty(FAGPERSON) String fagperson,
                    @JsonProperty(BIRTH_DATE_FIELD) String birthDate,
                    @JsonProperty(PERSONAL_NUMBER_FIELD) String personalNumber) {
        this.fsIdNumber = fsIdNumber;
        this.fagperson = fagperson;
        this.birthDate = birthDate;
        this.personalNumber = personalNumber;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getFsIdNumber(), getStaffPerson(), getBirthDate(), getPersonalNumber());
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

    public String getStaffPerson() {
        return fagperson;
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
        FsPerson fsPerson = (FsPerson) o;
        return fsIdNumber.equals(fsPerson.fsIdNumber) && fagperson.equals(fsPerson.fagperson) && birthDate.equals(
            fsPerson.birthDate) && personalNumber.equals(fsPerson.personalNumber);
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
