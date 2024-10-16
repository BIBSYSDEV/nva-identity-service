package no.unit.nva.useraccessservice.model;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.time.LocalDateTime;

public record TermsConditionsResponse(
    URI id,
    LocalDateTime validFrom
) implements JsonSerializable {

    public static final class TermsConditionsResponseBuilder {
        private URI id;
        private LocalDateTime validFrom;

        private TermsConditionsResponseBuilder() {
        }

        public static TermsConditionsResponseBuilder aTermsConditionsResponse() {
            return new TermsConditionsResponseBuilder();
        }


        public TermsConditionsResponseBuilder withId(URI id) {
            this.id = id;
            return this;
        }

        public TermsConditionsResponseBuilder withValidFrom(LocalDateTime validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public TermsConditionsResponse build() {
            return new TermsConditionsResponse(id, validFrom);
        }
    }
}
