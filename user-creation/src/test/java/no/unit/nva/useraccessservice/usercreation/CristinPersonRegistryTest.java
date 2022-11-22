package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_CREDENTIALS_SECRET_NAME;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_PASSWORD_SECRET_KEY;
import static no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry.CRISTIN_USERNAME_SECRET_KEY;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
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
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistryException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
public class CristinPersonRegistryTest {

    private PersonRegistry personRegistry;
    private FakeSecretsManagerClient secretsManagerClient;
    private AuthenticationScenarios scenarios;
    private LocalCustomerServiceDatabase customerServiceDatabase;
    private LocalIdentityService identityServiceDatabase;
    private IdentityService identityService;
    private DynamoDBCustomerService customerService;

    @BeforeEach
    public void beforeEach(WireMockRuntimeInfo wireMockRuntimeInfo) throws InvalidInputException, ConflictException {
        setupCustomerAndIdentityService();
        var httpClient = WiremockHttpClient.create();
        var cristinUsername = randomString();
        var cristinPassword = randomString();
        secretsManagerClient = new FakeSecretsManagerClient();
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_USERNAME_SECRET_KEY, cristinUsername);
        secretsManagerClient.putSecret(CRISTIN_CREDENTIALS_SECRET_NAME, CRISTIN_PASSWORD_SECRET_KEY, cristinPassword);
        var apiDomain = ServiceConstants.API_DOMAIN;
        var wiremockUri = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        personRegistry = CristinPersonRegistry.customPersonRegistry(httpClient,
                                                                    wiremockUri,
                                                                    apiDomain,
                                                                    new SecretsReader(secretsManagerClient));
        MockPersonRegistry mockPersonRegistry = new MockPersonRegistry(cristinUsername, cristinPassword, wiremockUri);
        scenarios = new AuthenticationScenarios(mockPersonRegistry, customerService, identityService);
    }

    @AfterEach
    public void afterEach() {
        customerServiceDatabase.deleteDatabase();
        identityServiceDatabase.closeDB();
    }

    @Test
    void shouldThrowExceptionIfCristinIsUnavailable(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var httpClient = WiremockHttpClient.create();
        var uriWhereCristinIsUnavailable
            = URI.create("https://localhost:" + (wireMockRuntimeInfo.getHttpsPort() - 1));
        personRegistry = CristinPersonRegistry.customPersonRegistry(httpClient,
                                                                    uriWhereCristinIsUnavailable,
                                                                    ServiceConstants.API_DOMAIN,
                                                                    new SecretsReader(secretsManagerClient));

        assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(randomString()));
    }

    @Test
    void shouldThrowExceptionIfCristinRespondsWithUnexpectedJson() {
        var personNin = scenarios.failingPersonRegistryRequestBadJson();

        assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(personNin));
    }

    @Test
    void shouldThrowExceptionIfCristinRespondsWithNonOkStatusCode() {
        var personNin = scenarios.failingPersonRegistryRequestBadGateway();

        assertThrows(PersonRegistryException.class, () -> personRegistry.fetchPersonByNin(personNin));
    }

    private void setupCustomerAndIdentityService() {
        customerServiceDatabase = new LocalCustomerServiceDatabase();
        customerServiceDatabase.setupDatabase();
        identityServiceDatabase = new LocalIdentityService();
        identityService = identityServiceDatabase.createDatabaseServiceUsingLocalStorage();
        customerService = new DynamoDBCustomerService(customerServiceDatabase.getDynamoClient());
    }
}
