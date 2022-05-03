package no.unit.nva.useraccessservice.usercreation.cristin.person;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.exceptions.ExceptionUtils;

@JacocoGenerated
public class CristinPersonResponse {

    @JsonProperty("id")
    private URI cristinId;
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

    @JacocoGenerated
    public URI getCristinId() {
        return cristinId;
    }

    @JacocoGenerated
    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    @JacocoGenerated
    public List<NameValue> getNames() {
        return names;
    }

    @JacocoGenerated
    public void setNames(List<NameValue> names) {
        this.names = names;
    }

    @JsonIgnore
    public CristinIdentifier getPersonsCristinIdentifier() {
        return Optional.of(getCristinId())
            .map(CristinIdentifier::fromCristinId)
            .orElseThrow();
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
        return Optional.ofNullable(nin).map(NationalIdentityNumber::getNin).orElseThrow();
    }

    @JacocoGenerated
    public void setNin(String nin) {
        this.nin = new NationalIdentityNumber(nin);
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElse(
            fail -> ExceptionUtils.stackTraceInSingleLine(fail.getException()));
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

        private final CristinPersonResponse cristinResponse;

        private Builder() {
            cristinResponse = new CristinPersonResponse();
            cristinResponse.setNames(new ArrayList<>());
        }

        public Builder withCristinId(URI cristinId) {
            if (nonNull(cristinId)) {
                cristinResponse.setCristinId(cristinId);
            }
            return this;
        }

        public Builder withAffiliations(List<CristinAffiliation> affiliations) {
            cristinResponse.setAffiliations(affiliations);
            return this;
        }

        public Builder withNin(NationalIdentityNumber nin) {
            cristinResponse.setNin(nonNull(nin) ? nin.getNin() : null);
            return this;
        }

        public Builder withFirstName(String name) {
            var nameValue = nonNull(name) ? NameValue.firstName(name) : null;
            cristinResponse.getNames().add(nameValue);
            return this;
        }

        public Builder withLastName(String name) {
            var nameValue = nonNull(name) ? NameValue.lastName(name) : null;
            cristinResponse.getNames().add(nameValue);
            return this;
        }

        public CristinPersonResponse build() {
            return cristinResponse;
        }
    }
}
