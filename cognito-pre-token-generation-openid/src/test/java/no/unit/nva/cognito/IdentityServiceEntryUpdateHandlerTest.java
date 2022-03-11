package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.FEIDE_ID;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FON_NON_FEIDE_USERS;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.NIN_FOR_FEIDE_USERS;
import static no.unit.nva.cognito.NetworkingUtils.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.CONTENT_TYPE;
import static no.unit.nva.cognito.NetworkingUtils.JWT_TOKEN_FIELD;
import static no.unit.nva.cognito.cristin.CristinClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomVocabularies;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsNot.not;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolEvent.CallerContext;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Request;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.CristinAffiliation;
import no.unit.nva.cognito.cristin.CristinResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.CustomerDynamoDBLocal;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class IdentityServiceEntryUpdateHandlerTest {

    public static final boolean MATCH_CASE = false;
    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final String RANDOM_NIN = randomString();
    public static final HttpClient HTTP_CLIENT = WiremockHttpClient.create();
    public static final boolean ACTIVE = true;
    public static final boolean INACTIVE = false;
    private static final FakeCognitoIdentityProviderClient COGNITO_CLIENT = new FakeCognitoIdentityProviderClient();
    private static final String CLIENT_SECRET = COGNITO_CLIENT.getFakeClientSecret();
    private static final String CLIENT_ID = COGNITO_CLIENT.getFakeClientId();
    private final Context context = new FakeContext();
    private IdentityServiceEntryUpdateHandler handler;

    private WireMockServer httpServer;
    private URI serverUri;
    private String jwtToken;
    private CustomerDynamoDBLocal customerDynamoDbLocal;
    private DynamoDBCustomerService customerService;

    public static Stream<CognitoUserPoolPreTokenGenerationEvent> eventProvider() {
        return Stream.of(randomEventOfFeideUser(), randomEventOfNonFeideUser());
    }

    @BeforeEach
    public void init() {
        setUpWiremock();
        this.jwtToken = setupCognitoMockResponse();
        var cognitoHost = this.serverUri;
        var cristinHost = this.serverUri;

        setupCustomerService();
        handler = new IdentityServiceEntryUpdateHandler(COGNITO_CLIENT,
                                                        HTTP_CLIENT,
                                                        cognitoHost,
                                                        cristinHost,
                                                        customerService);
    }

    @AfterEach
    public void close() {
        httpServer.stop();
        customerDynamoDbLocal.deleteDatabase();
    }

    @ParameterizedTest(name = "should return custom groups containing customer id and default access right when user "
                              + "does not exist in NVA's user base (first time login)")
    @MethodSource("eventProvider")
    @Disabled(value = "Not ready to implement this test yet.")
    void shouldCreateCustomUserEntriesInIdentityServiceForAllActiveCristinAffiliationsWhenPersonRequiresTokens(
        CognitoUserPoolPreTokenGenerationEvent event) {

    }

    @ParameterizedTest
    @MethodSource("eventProvider")
    void shouldReturnCustomGroupsContainingNvaCustomerIdForAllActiveCristinAffiliationsWhenPersonRequiresTokens(
        CognitoUserPoolPreTokenGenerationEvent event
    ) {
        var personsActiveAffiliations = List.of(randomUri(), randomUri());
        var personsInactiveAffiliations = List.of(randomUri(), randomUri());
        setupCristinServiceResponse(personsActiveAffiliations, personsInactiveAffiliations);
        var customers = populateCustomerService(personsActiveAffiliations, personsInactiveAffiliations);

        var expectedCustomGroups = customers.stream()
            .filter(customer -> personsActiveAffiliations.contains(URI.create(customer.getCristinId())))
            .map(CustomerDtoWithoutContext::getId)
            .map(URI::toString)
            .toArray(String[]::new);

        var response = handler.handleRequest(event, context);

        var actualCustomGroups = extractActualCustomGroups(response);
        assertThat(List.of(actualCustomGroups), containsInAnyOrder(expectedCustomGroups));
        assertThat(personsInactiveAffiliations, everyItem(not(in(actualCustomGroups))));
    }

    private static CognitoUserPoolPreTokenGenerationEvent randomEventOfFeideUser() {

        Map<String, String> userAttributes = Map.of(FEIDE_ID, randomString(), NIN_FOR_FEIDE_USERS, RANDOM_NIN);
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(CLIENT_ID).build())
            .build();
    }

    private static CognitoUserPoolPreTokenGenerationEvent randomEventOfNonFeideUser() {
        Map<String, String> userAttributes = Map.of(NIN_FON_NON_FEIDE_USERS, RANDOM_NIN);
        return CognitoUserPoolPreTokenGenerationEvent.builder()
            .withUserPoolId(randomString())
            .withUserName(randomString())
            .withRequest(Request.builder().withUserAttributes(userAttributes).build())
            .withCallerContext(CallerContext.builder().withClientId(CLIENT_ID).build())
            .build();
    }

    private List<CustomerDto> populateCustomerService(List<URI> personsActiveAffiliations,
                                                      List<URI> personsInactiveAffiliations) {
        return Stream.of(personsActiveAffiliations, personsInactiveAffiliations)
            .flatMap(Collection::stream)
            .map(this::createCustomer)
            .collect(Collectors.toList());
    }

    private void setupCustomerService() {
        this.customerDynamoDbLocal = new CustomerDynamoDBLocal();
        customerDynamoDbLocal.setupDatabase();
        var localCustomerClient = customerDynamoDbLocal.getDynamoClient();
        this.customerService = new DynamoDBCustomerService(localCustomerClient);
    }

    private CustomerDto createCustomer(URI affiliation) {
        var dto = CustomerDto.builder()
            .withCristinId(affiliation.toString())
            .withVocabularies(randomVocabularies())
            .withArchiveName(randomString())
            .withName(randomString())
            .withDisplayName(randomString())
            .build();
        return customerService.createCustomer(dto);
    }

    private void setUpWiremock() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }

    private String[] extractActualCustomGroups(CognitoUserPoolPreTokenGenerationEvent response) {
        return response.getResponse()
            .getClaimsOverrideDetails()
            .getGroupOverrideDetails()
            .getGroupsToOverride();
    }

    private String setupCognitoMockResponse() {
        var jwtToken = randomString();
        stubFor(post("/oauth2/token")
                    .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                    .withHeader(CONTENT_TYPE, wwwFormUrlEndcoded())
                    .withRequestBody(new ContainsPattern("grant_type"))
                    .willReturn(createCognitoResponse(jwtToken)));
        return jwtToken;
    }

    private ResponseDefinitionBuilder createCognitoResponse(String jwtToken) {
        var jsonMap = Map.of(JWT_TOKEN_FIELD, jwtToken);
        var responseBody = attempt(() -> JsonConfig.objectMapper.asString(jsonMap)).orElseThrow();
        return aResponse().withStatus(HTTP_OK).withBody(responseBody);
    }

    private void setupCristinServiceResponse(List<URI> personsAffiliations, List<URI> personInActiveAffiliations) {
        stubFor(post("/person/identityNumber")
                    .withHeader(AUTHORIZATION_HEADER, new EqualToPattern("Bearer " + jwtToken, MATCH_CASE))
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .withRequestBody(cristinServiceRequestBody())
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(cristinResponseBody(personsAffiliations,
                                                                                             personInActiveAffiliations))));
    }

    private String cristinResponseBody(List<URI> personsActiveAffiliations, List<URI> personsInactiveAffiliations) {
        var allAffiliations =
            Stream.of(createAffiliations(personsActiveAffiliations, ACTIVE),
                      createAffiliations(personsInactiveAffiliations, INACTIVE))
                .flatMap(Function.identity())
                .collect(Collectors.toList());
        return CristinResponse.builder()
            .withAffiliations(allAffiliations)
            .build()
            .toString();
    }

    private Stream<CristinAffiliation> createAffiliations(List<URI> personsActiveAffiliations, boolean active) {
        return personsActiveAffiliations.stream().map(uri -> createAffiliation(uri, active));
    }

    private CristinAffiliation createAffiliation(URI uri, boolean active) {
        return CristinAffiliation.builder()
            .withOrganization(uri)
            .withActive(active)
            .build();
    }

    private ContentPattern<?> cristinServiceRequestBody() {
        String jsonBody = String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, RANDOM_NIN);
        return new EqualToJsonPattern(jsonBody, IGNORE_ARRAY_ORDER, DO_NOT_IGNORE_OTHER_ELEMENTS);
    }

    private StringValuePattern applicationJson() {
        return new EqualToPattern("application/json", MATCH_CASE);
    }

    private EqualToPattern wwwFormUrlEndcoded() {
        return new EqualToPattern(APPLICATION_X_WWW_FORM_URLENCODED, MATCH_CASE);
    }
}