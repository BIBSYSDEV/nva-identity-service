package no.unit.nva.useraccess.events.service;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccess.events.client.BareProxyClientImpl.ERROR_READING_SECRETS_ERROR;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccess.events.client.BareProxyClient;
import no.unit.nva.useraccess.events.client.BareProxyClientImpl;
import no.unit.nva.useraccess.events.client.SimpleAuthorityResponse;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class UserMigrationServiceTest {

    private static final URI SAMPLE_ORG_ID = URI.create("https://localhost/cristin/organization/123.0.0.0");

    private CustomerService customerServiceMock;
    private BareProxyClient bareProxyClient;
    private UserMigrationService userMigrationService;
    private HttpClient httpClientMock;

    @BeforeEach
    public void init() {
        SecretsReader secretsReaderMock = mock(SecretsReader.class);
        when(secretsReaderMock.fetchSecret(anyString(), anyString())).thenReturn(randomString());
        customerServiceMock = mock(CustomerService.class);
        httpClientMock = mock(HttpClient.class);
        bareProxyClient = new BareProxyClientImpl(secretsReaderMock, httpClientMock);
        userMigrationService = new UserMigrationServiceImpl(customerServiceMock, bareProxyClient);
    }

    @Test
    void shouldReturnUserWithDefaultViewingScope() throws Exception {
        var customer = createSampleCustomer();
        when(customerServiceMock.getCustomer(any())).thenReturn(customer);
        prepareOkAndThenOkResponse(createSampleAuthorityResponse().toJson());

        var user = createSampleUser();
        var actualUser = userMigrationService.migrateUser(user);
        var expectedUser = user.copy().withViewingScope(defaultViewingScope(SAMPLE_ORG_ID)).build();

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    @Test
    void shouldUpdateBareOnceOnInvalidOrganizationId() throws Exception {
        var customer = createSampleCustomer();
        when(customerServiceMock.getCustomer(any())).thenReturn(customer);
        prepareOkAndThenOkResponse(createSampleAuthorityResponseWithInvalidOrganizationId().toJson());

        var user = createSampleUser();
        userMigrationService.migrateUser(user);

        verify(httpClientMock, times(2)).send(any(), any());
    }

    @Test
    void shouldNotUpdateBareOnValidOrganizationId() throws Exception {
        var customer = createSampleCustomer();
        when(customerServiceMock.getCustomer(any())).thenReturn(customer);
        prepareOkAndThenOkResponse(createSampleAuthorityResponse().toJson());

        var user = createSampleUser();
        userMigrationService.migrateUser(user);

        verify(httpClientMock, times(1)).send(any(), any());
    }

    @Test
    void shouldFinishEvenWhenBareUpdateReturnsBadGateway() throws Exception {
        var customer = createSampleCustomer();
        when(customerServiceMock.getCustomer(any())).thenReturn(customer);
        prepareOkResponseThenBadGatewayResponse(createSampleAuthorityResponseWithInvalidOrganizationId().toJson());

        var user = createSampleUser();
        userMigrationService.migrateUser(user);

        verify(httpClientMock, times(2)).send(any(), any());
    }

    @Test
    void shouldNotUpdateBareOnUserNotFound() throws Exception {
        var customer = createSampleCustomer();
        when(customerServiceMock.getCustomer(any())).thenReturn(customer);
        prepareNotFoundResponse();

        var user = createSampleUser();
        userMigrationService.migrateUser(user);

        verify(httpClientMock, times(1)).send(any(), any());
    }

    @Test
    void shouldLogMessageWhenSecretsFailToRead() throws ErrorReadingSecretException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        SecretsReader secretsReader = failingSecretsReader();
        HttpClient httpClient = HttpClient.newBuilder().build();
        Executable action = () -> new BareProxyClientImpl(secretsReader, httpClient);
        assertThrows(RuntimeException.class, action);
        assertThat(appender.getMessages(), containsString(ERROR_READING_SECRETS_ERROR));
    }

    @Test
    void shouldLogMessageWhenCustomerIdentifierIsNotValidUuid() {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        var user = createSampleUserWithInvalidCustomerId();
        var customerIdExpectedInLogMessage = user.getInstitution().toString();
        var usernameExpectedInLogMessage = user.getUsername();
        assertDoesNotThrow(() -> userMigrationService.migrateUser(user));
        assertThat(appender.getMessages(), containsString(customerIdExpectedInLogMessage));
        assertThat(appender.getMessages(), containsString(usernameExpectedInLogMessage));
    }

    private UserDto createSampleUserWithInvalidCustomerId() {
        return createSampleUser().copy().withInstitution(randomUri()).build();
    }

    private SecretsReader failingSecretsReader() throws ErrorReadingSecretException {
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(anyString(), anyString())).thenThrow(new ErrorReadingSecretException());
        return secretsReader;
    }

    private void prepareOkAndThenOkResponse(String body) throws IOException, InterruptedException {
        HttpResponse<String> okResponseWithBody = mock(HttpResponse.class);
        when(okResponseWithBody.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(okResponseWithBody.body()).thenReturn(body);
        HttpResponse<String> okResponseWithoutBody = mock(HttpResponse.class);
        when(okResponseWithoutBody.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpClientMock.<String>send(any(), any()))
            .thenReturn(okResponseWithBody)
            .thenReturn(okResponseWithoutBody);
    }

    private void prepareNotFoundResponse() throws IOException, InterruptedException {
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(httpClientMock.<String>send(any(), any())).thenReturn(httpResponse);
    }

    private void prepareOkResponseThenBadGatewayResponse(String body) throws IOException, InterruptedException {
        HttpResponse<String> okResponse = mock(HttpResponse.class);
        when(okResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(okResponse.body()).thenReturn(body);
        HttpResponse<String> badGatewayResponse = mock(HttpResponse.class);
        when(badGatewayResponse.statusCode()).thenReturn(HttpURLConnection.HTTP_BAD_GATEWAY);
        when(httpClientMock.<String>send(any(), any()))
            .thenReturn(okResponse)
            .thenReturn(badGatewayResponse);
    }

    private SimpleAuthorityResponse createSampleAuthorityResponseWithInvalidOrganizationId() {
        var authority = new SimpleAuthorityResponse();
        authority.setSystemControlNumber(randomString());
        authority.setOrganizationIds(List.of(randomUri()));
        return authority;
    }

    private SimpleAuthorityResponse createSampleAuthorityResponse() {
        var authority = new SimpleAuthorityResponse();
        authority.setSystemControlNumber(randomString());
        authority.setOrganizationIds(List.of(SAMPLE_ORG_ID));
        return authority;
    }

    private CustomerDto createSampleCustomer() {
        return CustomerDto.builder()
            .withCristinId(SAMPLE_ORG_ID.toString())
            .build();
    }

    private UserDto createSampleUser() {
        return UserDto.newBuilder()
            .withUsername(randomString())
            .withGivenName(randomString())
            .withFamilyName(randomString())
            .withInstitution(randomInstitutionUri())
            .build();
    }

    private URI randomInstitutionUri() {
        return URI.create("https://www.example.com/" + UUID.randomUUID());
    }
}
