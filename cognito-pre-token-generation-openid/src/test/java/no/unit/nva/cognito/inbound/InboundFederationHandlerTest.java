package no.unit.nva.cognito.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InboundFederationHandlerTest {

  /**
   * Plain Jackson, configured like the Lambda runtime's serializer: unknown properties are ignored
   * and nothing else is special-cased. nva-commons' dtoObjectMapper would turn empty strings into
   * null, which the runtime does not do.
   */
  private static final JsonMapper RUNTIME_LIKE_MAPPER =
      JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  private static final Path EVENT_RESOURCE = Path.of("cognito", "inbound_federation_event.json");
  private static final String NIN_CLAIM = "https://n.feide.no/claims/nin";
  private static final String PRINCIPAL_NAME_CLAIM =
      "https://n.feide.no/claims/eduPersonPrincipalName";
  private static final String NOR_EDU_PERSON_NIN = "norEduPersonNIN";
  private static final String HOME_ORGANIZATION = "schacHomeOrganization";
  private static final String SYNTHETIC_NIN = "01819000049";
  private static final String EMAIL = "example@example.no";
  private static final String FAIL_MESSAGE = "Login refused by NVA";
  private static final String SUB = "sub";

  private final FakeContext context = new FakeContext();
  private LogRecorder logRecorder;

  @BeforeEach
  void init() {
    logRecorder = LogRecorder.forClass(InboundFederationHandler.class);
  }

  @Test
  void shouldReturnTheEventWithAnEmptyMappingByDefault() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(), Optional.empty());

    var response = handler.handleRequest(event(), context);

    assertThat(response.getResponse().getUserAttributesToMap()).isEmpty();
  }

  @Test
  void shouldReadKeysToDropFromTheEnvironment() throws JsonProcessingException {
    var environment = mock(Environment.class);
    when(environment.readEnvOpt(InboundFederationHandler.DROP_KEYS_ENV))
        .thenReturn(Optional.of(" " + NIN_CLAIM + " ,, " + NOR_EDU_PERSON_NIN));
    when(environment.readEnvOpt(InboundFederationHandler.FAIL_MESSAGE_ENV))
        .thenReturn(Optional.empty());
    var handler = new InboundFederationHandler(environment);

    var mapping = handler.handleRequest(event(), context).getResponse().getUserAttributesToMap();

    assertThat(mapping).isNotEmpty().doesNotContainKeys(NIN_CLAIM, NOR_EDU_PERSON_NIN);
  }

  @Test
  void shouldKeepAllAttributesWhenTheEnvironmentIsUnset() throws JsonProcessingException {
    var environment = mock(Environment.class);
    when(environment.readEnvOpt(InboundFederationHandler.DROP_KEYS_ENV))
        .thenReturn(Optional.empty());
    when(environment.readEnvOpt(InboundFederationHandler.FAIL_MESSAGE_ENV))
        .thenReturn(Optional.empty());
    var handler = new InboundFederationHandler(environment);

    var response = handler.handleRequest(event(), context);

    assertThat(response.getResponse().getUserAttributesToMap()).isEmpty();
  }

  @Test
  void shouldLogAttributeKeysAndTypesOfNonScalarValues() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(), Optional.empty());

    handler.handleRequest(event(), context);

    assertThat(logRecorder.asString())
        .contains(
            "Inbound federation for Dataporten_76f8b7a1-2f2e-4d53-9a1e-0b2c3d4e5f60: provider"
                + " Dataporten (OIDC)",
            "tokenResponse: 3 key(s) [access_token, expires_in, token_type]",
            NIN_CLAIM,
            NOR_EDU_PERSON_NIN,
            HOME_ORGANIZATION,
            "nested=LinkedHashMap",
            "samlResponse: empty");
  }

  @Test
  void shouldNeverLogAnAttributeValueInDefaultMode() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(), Optional.empty());

    handler.handleRequest(event(), context);

    assertThat(logRecorder.asString()).doesNotContain(attributeValues());
  }

  @Test
  void shouldNeverLogAnAttributeValueWhenDroppingKeys() throws JsonProcessingException {
    var handler =
        new InboundFederationHandler(Set.of(NIN_CLAIM, NOR_EDU_PERSON_NIN), Optional.empty());

    handler.handleRequest(event(), context);

    assertThat(logRecorder.asString())
        .contains(
            "Dropping ["
                + NIN_CLAIM
                + ", "
                + NOR_EDU_PERSON_NIN
                + "] from the mapping, not mapping non-scalar [nested]")
        .doesNotContain(attributeValues());
  }

  @Test
  void shouldFailWithTheConfiguredMessage() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(), Optional.of(FAIL_MESSAGE));
    var event = event();

    assertThatThrownBy(() -> handler.handleRequest(event, context))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(FAIL_MESSAGE);
  }

  @Test
  void shouldNeverLogAnAttributeValueWhenFailing() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(), Optional.of(FAIL_MESSAGE));
    var event = event();

    catchThrowable(() -> handler.handleRequest(event, context));

    assertThat(logRecorder.asString()).doesNotContain(attributeValues());
  }

  @Test
  void shouldLogWhichMappedAttributesHaveNonBlankValues() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(), Optional.empty());

    handler.handleRequest(event(), context);

    assertThat(logRecorder.asString())
        .contains(
            "idToken: mapped attributes with a non-blank value: ["
                + NIN_CLAIM
                + ", "
                + PRINCIPAL_NAME_CLAIM
                + "]",
            "userInfo: mapped attributes with a non-blank value: [eduPersonAffiliation, "
                + NOR_EDU_PERSON_NIN
                + ", "
                + HOME_ORGANIZATION
                + ", name]");
  }

  @Test
  void shouldDropConfiguredKeysAndReturnTheRemainingScalarAttributes()
      throws JsonProcessingException {
    var handler =
        new InboundFederationHandler(Set.of(NIN_CLAIM, NOR_EDU_PERSON_NIN), Optional.empty());

    var mapping = handler.handleRequest(event(), context).getResponse().getUserAttributesToMap();

    assertThat(mapping)
        .doesNotContainKeys(NIN_CLAIM, NOR_EDU_PERSON_NIN, "nested")
        .containsEntry(SUB, "76f8b7a1-2f2e-4d53-9a1e-0b2c3d4e5f60")
        .containsEntry(PRINCIPAL_NAME_CLAIM, EMAIL)
        .containsEntry(HOME_ORGANIZATION, "example.no")
        .containsEntry("exp", "1760000000");
  }

  @Test
  void shouldLetIdTokenWinOverUserInfoForTheSameKey() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(NOR_EDU_PERSON_NIN), Optional.empty());

    var mapping = handler.handleRequest(event(), context).getResponse().getUserAttributesToMap();

    assertThat(mapping).containsEntry(NIN_CLAIM, SYNTHETIC_NIN);
  }

  @Test
  void shouldToleratePartsOfTheEventBeingNull() throws JsonProcessingException {
    var tree = (ObjectNode) RUNTIME_LIKE_MAPPER.readTree(eventJson());
    ((ObjectNode) tree.at("/request/attributes")).putNull("userInfo").putNull("samlResponse");
    tree.putNull("response").putNull("callerContext");
    var event = RUNTIME_LIKE_MAPPER.treeToValue(tree, InboundFederationEvent.class);
    var handler = new InboundFederationHandler(Set.of(SUB), Optional.empty());

    var mapping = handler.handleRequest(event, context).getResponse().getUserAttributesToMap();

    assertThat(mapping).containsEntry(PRINCIPAL_NAME_CLAIM, EMAIL).doesNotContainKey(SUB);
  }

  @Test
  void shouldSurviveAJsonRoundTrip() throws JsonProcessingException {
    var handler = new InboundFederationHandler(Set.of(SUB), Optional.empty());
    var response = handler.handleRequest(event(), context);

    var json = RUNTIME_LIKE_MAPPER.writeValueAsString(response);
    var reread = RUNTIME_LIKE_MAPPER.readValue(json, InboundFederationEvent.class);

    assertThat(reread.getResponse().getUserAttributesToMap())
        .isEqualTo(response.getResponse().getUserAttributesToMap());
  }

  private static InboundFederationEvent event() throws JsonProcessingException {
    return RUNTIME_LIKE_MAPPER.readValue(eventJson(), InboundFederationEvent.class);
  }

  private static String eventJson() {
    return IoUtils.stringFromResources(EVENT_RESOURCE);
  }

  /**
   * Every non-blank string and every number under request.attributes in the fixture, except the
   * sub, which legitimately appears in the user name.
   */
  private static String[] attributeValues() throws JsonProcessingException {
    var values = new ArrayList<String>();
    collectScalars(RUNTIME_LIKE_MAPPER.readTree(eventJson()).at("/request/attributes"), values);
    assertThat(values).hasSizeGreaterThan(10);
    return values.toArray(String[]::new);
  }

  private static void collectScalars(JsonNode node, List<String> values) {
    node.fields()
        .forEachRemaining(
            field -> {
              var value = field.getValue();
              if (value.isObject()) {
                collectScalars(value, values);
              } else if (isNumberOrNonBlankText(value) && !SUB.equals(field.getKey())) {
                values.add(value.asText());
              }
            });
  }

  private static boolean isNumberOrNonBlankText(JsonNode value) {
    return value.isNumber() || (value.isTextual() && !value.asText().isBlank());
  }
}
