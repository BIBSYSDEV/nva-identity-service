package no.unit.nva.cognito.inbound;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Set;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the client code a login uses once while the Lambda initializes ahead of the logins, so that
 * the first login on an instance does not pay for loading and compiling it. It reads only keys that
 * cannot exist and writes nothing. An instance initialized on demand skips it, since a login is
 * already waiting for that init. A step that fails is logged and skipped, so priming never fails
 * the init.
 */
final class ClientPriming {

  static final String INITIALIZATION_TYPE_VARIABLE = "AWS_LAMBDA_INITIALIZATION_TYPE";
  static final Set<String> INITIALIZED_AHEAD_OF_CALLS =
      Set.of("provisioned-concurrency", "snap-start");

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientPriming.class);
  private static final String PRIMED_MESSAGE = "Primed the clients during {} init, in ms: {}";
  private static final String STEP_FAILED_MESSAGE = "Priming the {} failed, continuing without it";
  private static final String PERSON_REGISTRY = "person registry";
  private static final String LINK_TABLE = "link table";
  private static final String CUSTOMER_TABLE = "customer table";
  private static final String USER_TABLE = "user table";
  private static final String UNUSED_USERNAME = "client-priming";
  private static final URI UNUSED_CRISTIN_ID = URI.create("https://example.org/client-priming");

  private final PersonRegistry personRegistry;
  private final IdentityService identityService;
  private final CustomerService customerService;
  private final Clock clock;

  ClientPriming(
      PersonRegistry personRegistry,
      IdentityService identityService,
      CustomerService customerService,
      Clock clock) {
    this.personRegistry = personRegistry;
    this.identityService = identityService;
    this.customerService = customerService;
    this.clock = clock;
  }

  /** The initialization type is the value Lambda sets in {@link #INITIALIZATION_TYPE_VARIABLE}. */
  void primeFor(String initializationType) {
    if (!isInitializedAheadOfCalls(initializationType)) {
      return;
    }
    var durations = new LinkedHashMap<String, Long>();
    durations.put(PERSON_REGISTRY, timed(PERSON_REGISTRY, personRegistry::prime));
    durations.put(LINK_TABLE, timed(LINK_TABLE, () -> identityService.fetchLink(UNUSED_USERNAME)));
    durations.put(CUSTOMER_TABLE, timed(CUSTOMER_TABLE, this::lookUpUnusedCustomer));
    durations.put(
        USER_TABLE,
        timed(USER_TABLE, () -> identityService.getUsersByCristinId(UNUSED_CRISTIN_ID)));
    LOGGER.info(PRIMED_MESSAGE, initializationType, durations);
  }

  private static boolean isInitializedAheadOfCalls(String initializationType) {
    return nonNull(initializationType) && INITIALIZED_AHEAD_OF_CALLS.contains(initializationType);
  }

  private long timed(String step, Runnable action) {
    var start = clock.instant();
    try {
      action.run();
    } catch (RuntimeException e) {
      LOGGER.warn(STEP_FAILED_MESSAGE, step, e);
    }
    return Duration.between(start, clock.instant()).toMillis();
  }

  private void lookUpUnusedCustomer() {
    try {
      customerService.getCustomerByCristinId(UNUSED_CRISTIN_ID);
    } catch (NotFoundException expected) {
      LOGGER.debug("No customer {}, as expected", UNUSED_CRISTIN_ID);
    }
  }
}
