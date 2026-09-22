package no.unit.nva.customer.model;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.beans.ConstructorProperties;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.customer.model.interfaces.RightsRetentionStrategy;
import nva.commons.core.JacocoGenerated;

public class RightsRetentionStrategyDto implements RightsRetentionStrategy, JsonSerializable {

  /**
   * JSON name {@code id} for the policy link, kept while existing clients migrate.
   *
   * @deprecated Use {@code policyUri}. Removed in NP-51737.
   */
  @Deprecated static final String LEGACY_ID_FIELD = "id";

  private static final String TYPE_FIELD = "type";
  private static final String POLICY_URI_FIELD = "policyUri";

  private final RightsRetentionStrategyType type;
  private final URI policyUri;

  public RightsRetentionStrategyDto(RightsRetentionStrategy retention) {
    this(retention.getType(), retention.getId());
  }

  public RightsRetentionStrategyDto(RightsRetentionStrategyType type, URI policyUri) {
    this.type = type;
    this.policyUri = policyUriOrNull(type, policyUri);
  }

  /**
   * Jackson creator that also reads the policy link under its old JSON name.
   *
   * @deprecated Exists only to accept {@code id} from clients that have not migrated to {@code
   *     policyUri}. Removed in NP-51737.
   */
  @Deprecated
  @ConstructorProperties({TYPE_FIELD, POLICY_URI_FIELD, LEGACY_ID_FIELD})
  private RightsRetentionStrategyDto(
      RightsRetentionStrategyType type, URI policyUri, URI legacyId) {
    this(type, nonNull(policyUri) ? policyUri : legacyId);
  }

  @Override
  public RightsRetentionStrategyType getType() {
    return type;
  }

  @JsonProperty(POLICY_URI_FIELD)
  public URI getPolicyUri() {
    return policyUri;
  }

  /**
   * Same value as {@link #getPolicyUri()}, serialized under the deprecated JSON name {@code id}.
   *
   * @deprecated Use {@link #getPolicyUri()}. Removed once the frontend, nva-publication-api and
   *     nva-commons read {@code policyUri}.
   */
  @Deprecated
  @Override
  @JsonProperty(LEGACY_ID_FIELD)
  public URI getId() {
    return policyUri;
  }

  @Override
  @JacocoGenerated
  public int hashCode() {
    return Objects.hash(type, policyUri);
  }

  @Override
  @JacocoGenerated
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RightsRetentionStrategyDto that = (RightsRetentionStrategyDto) o;
    return type == that.type && Objects.equals(policyUri, that.policyUri);
  }

  @Override
  @JacocoGenerated
  public String toString() {
    return toJsonString();
  }

  /**
   * No policy page when RRS is switched off, or when the URI is empty (the frontend sends {@code
   * ""} when switching RRS off).
   */
  private static URI policyUriOrNull(RightsRetentionStrategyType type, URI policyUri) {
    var enabled = nonNull(type) && type != RightsRetentionStrategyType.NullRightsRetentionStrategy;
    return enabled && nonNull(policyUri) && !policyUri.toString().isBlank() ? policyUri : null;
  }
}
