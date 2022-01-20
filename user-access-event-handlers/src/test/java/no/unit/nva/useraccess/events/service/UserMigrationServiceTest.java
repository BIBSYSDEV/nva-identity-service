package no.unit.nva.useraccess.events.service;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessmanagement.dao.UserDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserMigrationServiceTest {

    private static final URI SAMPLE_ORG_ID = URI.create("https://localhost/cristin/organization/123.0.0.0");

    private CustomerService customerService;
    private UserMigrationService userMigrationService;

    @BeforeEach
    public void init() {
        customerService = Mockito.mock(CustomerService.class);
        userMigrationService = new UserMigrationServiceImpl(customerService);
    }

    @Test
    void shouldReturnUserWithDefaultViewingScope() throws Exception {
        var customer = getCustomer();
        when(customerService.getCustomer(any())).thenReturn(customer);

        UserDb user = getUser();
        UserDb updatedUser = userMigrationService.migrateUser(user);

        assertThat(updatedUser, is(notNullValue()));
        assertThat(updatedUser.getViewingScope().getIncludedUnits(), contains(SAMPLE_ORG_ID));
    }

    private CustomerDto getCustomer() {
        return CustomerDto.builder()
                .withCristinId(SAMPLE_ORG_ID.toString())
                .build();
    }

    private UserDb getUser() {
        return UserDb.newBuilder()
                .withUsername(randomString())
                .withGivenName(randomString())
                .withFamilyName(randomString())
                .withInstitution(randomUUID().toString())
                .build();
    }

}
