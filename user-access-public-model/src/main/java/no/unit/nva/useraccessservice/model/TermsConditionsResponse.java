package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import no.unit.nva.commons.json.JsonUtils;

import java.net.URI;

@JsonSerialize
public record TermsConditionsResponse(
    URI termsConditionsUri
) {
@Override
public String toString() {
    try {
        return JsonUtils.singleLineObjectMapper.writeValueAsString(this);
    } catch (Exception var2) {
        Exception e = var2;
        throw new RuntimeException(e);
    }
}

    public static TermsConditionsResponse.Builder builder() {
        return new TermsConditionsResponse.Builder();
    }

    public static class Builder {

        private URI termsConditionsUri;

        public TermsConditionsResponse.Builder withTermsConditionsUri(URI termsConditionsUri) {
            this.termsConditionsUri = termsConditionsUri;
            return this;
        }

        public TermsConditionsResponse build() {
            return new TermsConditionsResponse(termsConditionsUri);
        }
    }
}
