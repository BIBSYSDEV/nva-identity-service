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
 * The customers a person has an active affiliation with, as the person registry reports it. An
 * institution that is not a customer is the common case and is skipped, as is a customer that is no
 * longer active. Any other failure to read a customer propagates, so that a login never goes on
 * with a partial view of the person's customers.
 */
public final class ActiveCustomers {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveCustomers.class);
  private static final String NOT_A_CUSTOMER_MESSAGE =
      "No customer for institution {} of person {}";
  private static final String CUSTOMER_IS_INACTIVE_MESSAGE =
      "Customer is inactive {} for person {}";

  private ActiveCustomers() {}

  public static Set<CustomerDto> forPerson(Person person, CustomerService customerService) {
    return person.getAffiliations().stream()
        .map(Affiliation::getInstitutionId)
        .map(institutionId -> fetchCustomer(customerService, institutionId, person))
        .flatMap(Optional::stream)
        .filter(customer -> isActive(customer, person))
        .collect(Collectors.toSet());
  }

  private static Optional<CustomerDto> fetchCustomer(
      CustomerService customerService, URI institutionId, Person person) {
    try {
      return Optional.of(customerService.getCustomerByCristinId(institutionId));
    } catch (NotFoundException e) {
      LOGGER.info(NOT_A_CUSTOMER_MESSAGE, institutionId, person.getId());
      return Optional.empty();
    }
  }

  private static boolean isActive(CustomerDto customer, Person person) {
    if (!customer.isActive()) {
      LOGGER.info(CUSTOMER_IS_INACTIVE_MESSAGE, customer.getId(), person.getId());
    }
    return customer.isActive();
  }
}
