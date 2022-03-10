package no.unit.nva.cognito.cristin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CristinResponse {

    public static final String CRISTIN_IDENTIFIER_TYPE="CristinIdentifier";

    @JsonProperty("identifiers")
    private List<TypedValue> identifiers;
    @JsonProperty("affiliations")
    private List<CristinAffiliation> affiliations;
    @JsonProperty("NationalIdentificationNumber")
    private String nin;

    @JacocoGenerated
    public List<TypedValue> getIdentifiers() {
        return identifiers;
    }

    @JacocoGenerated
    public void setIdentifiers(List<TypedValue> identifiers) {
        this.identifiers = identifiers;
    }

    @JacocoGenerated
    public List<CristinAffiliation> getAffiliations() {
        return affiliations;
    }

    @JacocoGenerated
    public void setAffiliations(List<CristinAffiliation> affiliations) {
        this.affiliations = affiliations;
    }

    @JacocoGenerated
    public String getNin() {
        return nin;
    }

    @JacocoGenerated
    public void setNin(String nin) {
        this.nin = nin;
    }
}
