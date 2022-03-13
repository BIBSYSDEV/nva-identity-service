package no.unit.nva.cognito.cristin;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

@JacocoGenerated
public class CristinResponse {

    @JsonProperty("identifiers")
    private List<CristinIdentifier> identifiers;
    @JsonProperty("affiliations")
    private List<CristinAffiliation> affiliations;
    @JsonProperty("NationalIdentificationNumber")
    private NationalIdentityNumber nin;
    @JsonProperty("names")
    private List<NameValue> names;

    public static Builder builder() {
        return new Builder();
    }

    public static List<CristinIdentifier> createCristinIdentifier(String identifier) {
        return List.of(new CristinIdentifier(identifier));
    }

    public List<NameValue> getNames() {
        return names;
    }

    public void setNames(List<NameValue> names) {
        this.names = names;
    }

    @JacocoGenerated
    public List<CristinIdentifier> getIdentifiers() {
        return identifiers;
    }

    @JacocoGenerated
    public void setIdentifiers(List<CristinIdentifier> candidates) {
        this.identifiers = CristinIdentifier.selectFromCandidates(candidates);
    }

    @JsonIgnore
    public CristinIdentifier getPersonsCristinIdentifier() {
        return identifiers.stream().collect(SingletonCollector.collect());
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
        return nin.getNin();
    }

    public void setNin(NationalIdentityNumber nin) {
        this.nin = nin;
    }

    @JacocoGenerated
    public void setNin(String nin) {
        this.nin = new NationalIdentityNumber(nin);
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.objectMapper.asString(this)).orElseThrow();
    }

    @JsonIgnore
    public String extractFirstName() {
        return names.stream().filter(NameValue::isFirstName).map(NameValue::getValue).findFirst().orElse(null);
    }

    @JsonIgnore
    public String extractLastName() {
        return names.stream().filter(NameValue::isLastName).map(NameValue::getValue).findFirst().orElse(null);
    }

    public static final class Builder {

        private final CristinResponse cristinResponse;

        private Builder() {
            cristinResponse = new CristinResponse();
            cristinResponse.setNames(new ArrayList<>());
        }

        public Builder withIdentifiers(List<CristinIdentifier> identifiers) {
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

        public Builder withFirstName(String name){
            var firstName = NameValue.firstName(name);
            cristinResponse.getNames().add(firstName);
            return this;
        }


        public Builder withLastName(String name){
            var firstName = NameValue.lastName(name);
            cristinResponse.getNames().add(firstName);
            return this;
        }

        public CristinResponse build() {
            return cristinResponse;
        }
    }
}
