package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import no.unit.nva.identityservice.json.JsonConfig;
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

    public static Builder builder(){
        return new Builder();

    }
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

    public static final class Builder {

        private CristinResponse cristinResponse;

        private Builder() {
            cristinResponse = new CristinResponse();
        }

        public Builder withIdentifiers(List<TypedValue> identifiers) {
            cristinResponse.setIdentifiers(identifiers);
            return this;
        }

        public Builder withAffiliations(List<CristinAffiliation> affiliations) {
            cristinResponse.setAffiliations(affiliations);
            return this;
        }

        public Builder withNin(String nin) {
            cristinResponse.setNin(nin);
            return this;
        }

        public CristinResponse build() {
            return cristinResponse;
        }
    }

    public String toString(){
        return attempt(()->JsonConfig.objectMapper.asString(this)).orElseThrow();
    }

}
