package no.unit.nva.customer.model;

import static no.unit.nva.customer.model.RightsRetentionStrategyType.NullRightsRetentionStrategy;
import static no.unit.nva.customer.model.RightsRetentionStrategyType.OverridableRightsRetentionStrategy;
import static no.unit.nva.customer.model.RightsRetentionStrategyType.RightsRetentionStrategy;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Set;
import no.unit.nva.identityservice.json.JsonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RightsRetentionStrategyDtoTest {

  @Test
  void shouldSerializePolicyUriAndDeprecatedIdWithTheSameValue() throws IOException {
    var policyUri = randomUri();
    var dto = new RightsRetentionStrategyDto(OverridableRightsRetentionStrategy, policyUri);

    var json = JsonConfig.mapFrom(JsonConfig.writeValueAsString(dto));

    assertEquals(OverridableRightsRetentionStrategy.name(), json.get("type"));
    assertEquals(policyUri.toString(), json.get("policyUri"));
    assertEquals(policyUri.toString(), json.get("id"));
  }

  @Test
  void shouldOmitPolicyUriAndDeprecatedIdWhenNoPolicyUriIsSet() throws IOException {
    var dto = new RightsRetentionStrategyDto(NullRightsRetentionStrategy, null);

    var json = JsonConfig.mapFrom(JsonConfig.writeValueAsString(dto));

    assertEquals(Set.of("type"), json.keySet());
  }

  @ParameterizedTest
  @ValueSource(strings = {"policyUri", "id"})
  void shouldDeserializePolicyUriFromEitherJsonName(String jsonName) throws IOException {
    var policyUri = randomUri();
    var json = "{\"type\":\"RightsRetentionStrategy\",\"" + jsonName + "\":\"" + policyUri + "\"}";

    var dto = JsonConfig.readValue(json, RightsRetentionStrategyDto.class);

    assertEquals(RightsRetentionStrategy, dto.getType());
    assertEquals(policyUri, dto.getPolicyUri());
  }

  @Test
  void shouldTreatEmptyDeprecatedIdAsNoPolicyUri() throws IOException {
    var json = "{\"type\":\"NullRightsRetentionStrategy\",\"id\":\"\"}";

    var dto = JsonConfig.readValue(json, RightsRetentionStrategyDto.class);

    assertNull(dto.getPolicyUri());
    assertEquals(Set.of("type"), JsonConfig.mapFrom(JsonConfig.writeValueAsString(dto)).keySet());
  }

  @Test
  void shouldPreferPolicyUriOverDeprecatedIdWhenBothArePresent() throws IOException {
    var policyUri = randomUri();
    var legacyId = randomUri();
    var json =
        "{\"type\":\"RightsRetentionStrategy\",\"policyUri\":\""
            + policyUri
            + "\",\"id\":\""
            + legacyId
            + "\"}";

    var dto = JsonConfig.readValue(json, RightsRetentionStrategyDto.class);

    assertEquals(policyUri, dto.getPolicyUri());
  }
}
