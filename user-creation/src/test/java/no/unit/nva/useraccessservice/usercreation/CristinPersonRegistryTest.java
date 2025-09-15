package no.unit.nva.useraccessservice.usercreation;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.customer.testing.LocalCustomerServiceDatabase;
import no.unit.nva.database.IdentityService;
import no.unit.nva.database.LocalIdentityService;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.useraccessservice.constants.ServiceConstants;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.userceation.testing.cristin.AuthenticationScenarios;
import no.unit.nva.useraccessservice.userceation.testing.cristin.MockPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistryException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.HttpHeaders;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.logutils.LogUtils;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.constants.ServiceConstants.BOT_FILTER_BYPASS_HEADER_VALUE;
import static no.unit.nva.useraccessservice.userceation.testing.cristin.RandomNin.randomNin;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest(httpsEnabled = true)
class CristinPersonRegistryTest {

    private static final String BOT_FILTER_BYPASS_HEADER_NAME = randomString();
    private PersonRegistry personRegistry;
    private FakeSecretsManagerClient secretsManagerClient;
    private AuthenticationScenarios scenarios;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private LocalIdentityService identityServiceDatabase;
    private IdentityService identityService;
    private DynamoDBCustomerService customerService;
    private MockPersonRegistry mockPersonRegistry;

    @BeforeEach
    void beforeEach(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        setupCustomerAndIdentityService();
        var cristinUsername = randomString();
        var cristinPassword = randomString();
        secretsManagerClient = new FakeSecretsManagerClient();
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);
        var apiDomain = ServiceConstants.API_DOMAIN;
        var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        var httpClient = WiremockHttpClient.create();
        var defaultRequestHeaders = new HttpHeaders()
                                        .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
        personRegistry = CristinPersonRegistry.customPersonRegistry(httpClient,
                                                                    wiremockUri,
                                                                    apiDomain,
                                                                    defaultRequestHeaders,
                                                                    new SecretsReader(secretsManagerClient));
        this.mockPersonRegistry = new MockPersonRegistry(cristinUsername,
                                                                       cristinPassword,
                                                                       wiremockUri,
                                                                       defaultRequestHeaders);
        scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
    }

    private void setupCustomerAndIdentityService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        identityServiceDatabase = new LocalIdentityService();
        identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
        customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }

    @AfterEach
    void afterEach() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
    }

    @Test
    void shouldThrowExceptionIfCristinIsUnavailable(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var httpClient = WiremockHttpClient.create();
        var uriWhereCristinIsUnavailable
            = URI.create("https://localhost:" + (wireMockRuntimeInfo.getHttpsPort() - 1));

        var defaultRequestHeaders = new HttpHeaders()
                                        .withHeader(BOT_FILTER_BYPASS_HEADER_NAME, BOT_FILTER_BYPASS_HEADER_VALUE);
        personRegistry = CristinPersonRegistry.customPersonRegistry(httpClient,
                                                                    uriWhereCristinIsUnavailable,
                                                                    ServiceConstants.API_DOMAIN,
                                                                    defaultRequestHeaders,
                                                                    new SecretsReader(secretsManagerClient));
        var nin = NationalIdentityNumber.fromString(randomNin());
        var exception = assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(nin));
        assertThat(exception.getMessage(), not(containsString(nin.toString())));
        assertThat(appender.getMessages(), not(containsString(nin.toString())));
    }

    @Test
    void fetchPersonByIdentifierShouldReturnPersonIfExisting() {
        var person = scenarios.personWithoutAffiliations();

        var fetchedPerson = personRegistry.fetchPersonByIdentifier(person.cristinIdentifier());
        assertThat(fetchedPerson.isPresent(), is(equalTo(true)));
    }

    @Test
    void createPersonTest() {
        var person = scenarios.personThatIsNotRegisteredInPersonRegistry();
        mockPersonRegistry.createPostPersonStub(person.getCristinPerson());

        var fetchedPerson = personRegistry.createPerson(NationalIdentityNumber.fromString(person.nin()),
                                                        randomString(), randomString());
        assertThat(fetchedPerson.isPresent(), is(equalTo(true)));
    }

    @Test
    void fetchPersonByIdentifierShouldReturnEmptyOptionalIfNotExist() {
        var fetchedPerson = personRegistry.fetchPersonByIdentifier(randomString());
        assertThat(fetchedPerson.isPresent(), is(equalTo(false)));
    }

    @Test
    void shouldThrowExceptionIfCristinRespondsWithUnexpectedJson() {
        var personNin = scenarios.failingPersonRegistryRequestBadJson().nin();
        var nin = NationalIdentityNumber.fromString(personNin);

        assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(nin));
    }

    @Test
    void shouldThrowExceptionIfCristinRespondsWithNonOkStatusCode() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var personNin = scenarios.failingPersonRegistryRequestBadGateway().nin();
        var nin = NationalIdentityNumber.fromString(personNin);

        assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(nin));
        var expectedMaskedNin = "XXXXXXXXX" + personNin.substring(personNin.length() - 2);
        assertThat(appender.getMessages(), containsString(expectedMaskedNin));
    }

    @Test
    void shouldMaskNationalIdentityNumberInLog() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var personNin = "12345678901";
        var nin = NationalIdentityNumber.fromString(personNin);
        
        mockPersonRegistry.setupServerErrorForNin(personNin);

        assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(nin));
        assertThat(appender.getMessages(), containsString("XXXXXXXXX01"));
    }

    @Test
    void shouldReturnEmptyListOfAffiliationsIfFieldIsMissingInCristinOnGetPerson() {
        var personNin = scenarios.personWithoutAffiliations().nin();
        var nin = NationalIdentityNumber.fromString(personNin);

        var person = personRegistry.fetchPersonByNin(nin);
        assertThat(person.isPresent(), is(equalTo(true)));
        assertThat(person.get().getAffiliations(), emptyIterable());
    }
}
