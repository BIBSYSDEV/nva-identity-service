package no.unit.nva.useraccess.events.service;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserMigrationServiceTest {

    private static final URI SAMPLE_ORG_ID = URI.create("https://localhost/cristin/organization/123.0.0.0");
    private CustomerService customerServiceMock;
    private UserMigrationService userMigrationService;

    @BeforeEach
    public void init() {
        SecretsReader secretsReaderMock = mock(SecretsReader.class);
        when(secretsReaderMock.fetchSecret(anyString(), anyString())).thenReturn(randomString());
        customerServiceMock = mock(CustomerService.class);
        userMigrationService = new UserMigrationServiceImpl(customerServiceMock);
    }

    @Test
    void shouldReturnUserWithDefaultViewingScope() throws Exception {
        var customer = createSampleCustomer();
        when(customerServiceMock.getCustomer(any(URI.class))).thenReturn(customer);
        when(customerServiceMock.getCustomer(any(UUID.class))).thenReturn(customer);

        var user = createSampleUser();
        var actualUser = userMigrationService.migrateUser(user.copy().build());
        var expectedUser = user.copy().withViewingScope(defaultViewingScope(SAMPLE_ORG_ID)).build();

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    private CustomerDto createSampleCustomer() {
        return CustomerDto.builder()
            .withCristinId(SAMPLE_ORG_ID)
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

    @Test
    void shouldLogMessageWhenCustomerIdentifierIsNotValidUuid() throws IOException, InterruptedException {
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
}
