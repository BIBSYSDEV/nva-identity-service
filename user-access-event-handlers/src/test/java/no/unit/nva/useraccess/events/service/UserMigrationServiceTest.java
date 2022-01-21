package no.unit.nva.useraccess.events.service;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.useraccessmanagement.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserMigrationServiceTest {

    private static final URI SAMPLE_ORG_ID = URI.create("https://localhost/cristin/organization/123.0.0.0");

    private CustomerService customerService;
    private UserMigrationService userMigrationService;

    @BeforeEach
    public void init() {
        customerService = mock(CustomerService.class);
        userMigrationService = new UserMigrationServiceImpl(customerService);
    }

    @Test
    void shouldReturnUserWithDefaultViewingScope() throws Exception {
        var customer = createSampleCustomerDto();
        when(customerService.getCustomer(any())).thenReturn(customer);

        var user = createSampleUserDb();
        var actualUser = userMigrationService.migrateUser(user);
        var expectedUser = user.copy().withViewingScope(defaultViewingScope(SAMPLE_ORG_ID)).build();

        assertThat(actualUser, is(equalTo(expectedUser)));
    }

    private CustomerDto createSampleCustomerDto() {
        return CustomerDto.builder()
                .withCristinId(SAMPLE_ORG_ID.toString())
                .build();
    }

    private UserDto createSampleUserDb() {
        return UserDto.newBuilder()
                .withUsername(randomString())
                .withGivenName(randomString())
                .withFamilyName(randomString())
                .withInstitution(randomUUID().toString())
                .build();
    }

}
