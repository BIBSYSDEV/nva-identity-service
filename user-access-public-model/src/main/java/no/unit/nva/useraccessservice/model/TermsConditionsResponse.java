package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.time.Instant;

@JsonSerialize
public record TermsConditionsResponse(
    URI termsConditionsUri,
    Instant validFrom
) implements JsonSerializable {


    public static TermsConditionsResponse.Builder builder() {
        return new TermsConditionsResponse.Builder();
    }

    public static class Builder {

        private URI termsConditionsUri;
        private Instant validFrom;

        public TermsConditionsResponse.Builder withTermsConditionsUri(URI termsConditionsUri) {
            this.termsConditionsUri = termsConditionsUri;
            return this;
        }

        public TermsConditionsResponse.Builder withValidFrom(Instant validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public TermsConditionsResponse build() {
            return new TermsConditionsResponse(termsConditionsUri, validFrom);
        }
    }
}
