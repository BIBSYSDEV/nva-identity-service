package no.unit.nva.cognito.inbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

/** The event mirrors the documented JSON, so every documented field must survive the runtime. */
class InboundFederationEventTest {

  private static final JsonMapper RUNTIME_LIKE_MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
  private static final Path EVENT_RESOURCE = Path.of("cognito", "inbound_federation_event.json");

  @Test
  void shouldReadEveryDocumentedFieldOfTheEvent() throws JsonProcessingException {
    var event = RUNTIME_LIKE_MAPPER.readValue(eventJson(), InboundFederationEvent.class);

    assertThat(event.getVersion()).isEqualTo("1");
    assertThat(event.getTriggerSource()).isEqualTo("InboundFederation_ExternalProvider");
    assertThat(event.getRegion()).isEqualTo("eu-west-1");
    assertThat(event.getUserPoolId()).isEqualTo("eu-west-1_XXXXXXXXX");
    assertThat(event.getUserName()).isEqualTo("76f8b7a1-2f2e-4d53-9a1e-0b2c3d4e5f60");
    assertThat(event.getCallerContext().getAwsSdkVersion()).isEqualTo("aws-sdk-unknown-unknown");
    assertThat(event.getCallerContext().getClientId()).isEqualTo("1234567890abcdefghijklmnop");
    assertThat(event.getRequest().getProviderName()).isEqualTo("Dataporten");
    assertThat(event.getRequest().getProviderType()).isEqualTo("OIDC");
    assertThat(event.getRequest().getAttributes().getTokenResponse()).containsKey("access_token");
    assertThat(event.getRequest().getAttributes().getIdToken()).containsKey("sub");
    assertThat(event.getRequest().getAttributes().getUserInfo()).containsKey("sub");
    assertThat(event.getRequest().getAttributes().getSamlResponse()).isEmpty();
    assertThat(event.getResponse().getUserAttributesToMap()).isEmpty();
  }

  @Test
  void shouldKeepEmptyDefaultsWhereTheEventHasNulls() throws JsonProcessingException {
    var tree = (ObjectNode) RUNTIME_LIKE_MAPPER.readTree(eventJson());
    ((ObjectNode) tree.at("/request/attributes"))
        .putNull("userInfo")
        .putNull("samlResponse")
        .putNull("tokenResponse")
        .putNull("idToken");
    tree.putNull("response").putNull("callerContext");

    var event = RUNTIME_LIKE_MAPPER.treeToValue(tree, InboundFederationEvent.class);

    assertThat(event.getCallerContext()).isNull();
    assertThat(event.getResponse().getUserAttributesToMap()).isEmpty();
    assertThat(event.getRequest().getProviderName()).isEqualTo("Dataporten");
    assertThat(event.getRequest().getAttributes().getSamlResponse()).isEmpty();
    assertThat(event.getRequest().getAttributes().getUserInfo()).isEmpty();
    assertThat(event.getRequest().getAttributes().getIdToken()).isEmpty();
    assertThat(event.getRequest().getAttributes().getTokenResponse()).isEmpty();

    event.setRequest(null);
    assertThat(event.getRequest().getAttributes().getIdToken()).isEmpty();
  }

  private static String eventJson() {
    return IoUtils.stringFromResources(EVENT_RESOURCE);
  }
}
