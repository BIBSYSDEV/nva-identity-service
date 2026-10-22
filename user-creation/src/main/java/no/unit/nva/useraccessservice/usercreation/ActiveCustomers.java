package no.unit.nva.useraccessservice.usercreation;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The customers a person can act for. A customer that no longer exists is skipped, as is one that
 * is no longer active. Any other failure to read a customer propagates, so that a login never goes
 * on with a partial view of the person's customers.
 */
public final class ActiveCustomers {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveCustomers.class);
  private static final String NOT_A_CUSTOMER_MESSAGE =
      "No customer for institution {} of person {}";
  private static final String NO_SUCH_CUSTOMER_MESSAGE = "No customer {} remembered for person {}";
  private static final String CUSTOMER_IS_INACTIVE_MESSAGE =
      "Customer is inactive {} for person {}";

  private ActiveCustomers() {}

  /** From the person's active affiliations, as the person registry reports them. */
  public static Set<CustomerDto> forPerson(Person person, CustomerService customerService) {
    return person.getAffiliations().stream()
        .map(Affiliation::getInstitutionId)
        .map(institutionId -> fetchByInstitution(customerService, institutionId, person.getId()))
        .flatMap(Optional::stream)
        .filter(customer -> isActive(customer, person.getId()))
        .collect(Collectors.toSet());
  }

  /** From customer identifiers remembered for the person, as the link rows hold them. */
  public static Set<CustomerDto> byIds(
      Set<URI> customerIds, CustomerService customerService, URI personId) {
    return customerIds.stream()
        .map(customerId -> fetchById(customerService, customerId, personId))
        .flatMap(Optional::stream)
        .filter(customer -> isActive(customer, personId))
        .collect(Collectors.toSet());
  }

  private static Optional<CustomerDto> fetchByInstitution(
      CustomerService customerService, URI institutionId, URI personId) {
    try {
      return Optional.of(customerService.getCustomerByCristinId(institutionId));
    } catch (NotFoundException e) {
      LOGGER.info(NOT_A_CUSTOMER_MESSAGE, institutionId, personId);
      return Optional.empty();
    }
  }

  private static Optional<CustomerDto> fetchById(
      CustomerService customerService, URI customerId, URI personId) {
    try {
      return Optional.of(customerService.getCustomer(customerId));
    } catch (NotFoundException e) {
      LOGGER.warn(NO_SUCH_CUSTOMER_MESSAGE, customerId, personId);
      return Optional.empty();
    }
  }

  private static boolean isActive(CustomerDto customer, URI personId) {
    if (!customer.isActive()) {
      LOGGER.info(CUSTOMER_IS_INACTIVE_MESSAGE, customer.getId(), personId);
    }
    return customer.isActive();
  }
}
