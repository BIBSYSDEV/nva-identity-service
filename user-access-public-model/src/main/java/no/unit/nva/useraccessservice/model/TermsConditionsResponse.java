package no.unit.nva.useraccessservice.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;

@JsonSerialize
public record TermsConditionsResponse(URI termsConditionsUri) implements JsonSerializable {

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
