package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActiveCustomersTest {

  private CustomerService customerService;
  private LogRecorder logRecorder;

  @BeforeEach
  void init() {
    customerService = mock(CustomerService.class);
    logRecorder = LogRecorder.forClass(ActiveCustomers.class);
  }

  @Test
  void shouldGiveTheActiveCustomersOfThePersonsInstitutions() throws NotFoundException {
    var customerInstitution = randomUri();
    var customer = customer(customerInstitution, null);
    when(customerService.getCustomerByCristinId(customerInstitution)).thenReturn(customer);

    var customers =
        ActiveCustomers.forPerson(personAffiliatedWith(customerInstitution), customerService);

    assertThat(customers).containsExactly(customer);
  }

  @Test
  void shouldSkipAnInstitutionThatIsNotACustomer() throws NotFoundException {
    var notACustomer = randomUri();
    when(customerService.getCustomerByCristinId(notACustomer))
        .thenThrow(new NotFoundException("No customer"));
    var person = personAffiliatedWith(notACustomer);

    assertThat(ActiveCustomers.forPerson(person, customerService)).isEmpty();
    assertThat(logRecorder.asString())
        .contains("No customer for institution " + notACustomer + " of person " + person.getId());
  }

  @Test
  void shouldSkipACustomerThatIsNoLongerActive() throws NotFoundException {
    var institution = randomUri();
    var inactive = customer(institution, Instant.now().minusSeconds(1));
    when(customerService.getCustomerByCristinId(institution)).thenReturn(inactive);
    var person = personAffiliatedWith(institution);

    assertThat(ActiveCustomers.forPerson(person, customerService)).isEmpty();
    assertThat(logRecorder.asString())
        .contains("Customer is inactive " + inactive.getId() + " for person " + person.getId());
  }

  @Test
  void shouldFailWhenACustomerCannotBeRead() throws NotFoundException {
    var institution = randomUri();
    when(customerService.getCustomerByCristinId(institution))
        .thenThrow(new RuntimeException("Table unavailable"));
    var person = personAffiliatedWith(institution);

    assertThatThrownBy(() -> ActiveCustomers.forPerson(person, customerService))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Table unavailable");
  }

  private static Person personAffiliatedWith(URI institutionId) {
    return new Person(
        randomUri(),
        randomString(),
        randomString(),
        randomString(),
        List.of(new Affiliation(institutionId, List.of(randomUri()))));
  }

  private static CustomerDto customer(URI cristinId, Instant inactiveFrom) {
    return CustomerDto.builder()
        .withIdentifier(UUID.randomUUID())
        .withCristinId(cristinId)
        .withName(randomString())
        .withInactiveFrom(inactiveFrom)
        .build();
  }
}
