package no.unit.nva.cognito.inbound;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.DatabaseConfig.DEFAULT_DYNAMO_CLIENT;
import static no.unit.nva.database.IdentityService.defaultIdentityService;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.IdentityLink;
import no.unit.nva.useraccessservice.model.LinkedPerson;
import no.unit.nva.useraccessservice.usercreation.ActiveCustomers;
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceAlreadyExistsException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceLoginNotCompletedException;
import no.unit.nva.useraccessservice.usercreation.person.cristin.exceptions.IdentityServiceMissingNinException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cognito inbound federation trigger. Runs at every federated login, before Cognito stores the
 * user, and is the only code that sees the national identity number the identity provider sends. It
 * resolves the person in the person registry, creates the user entries, and writes the link and
 * person rows that the pre-token Lambda resolves the Cognito user from. The number is neither
 * stored nor logged: the provider mapping in the template leaves it out of the Cognito user, and
 * the response is always an empty mapping, which keeps the mapped attributes as they are.
 *
 * <p>No number from the provider gives error 1003, which the client answers by offering another
 * login method. Everything else gives 1005, except that a person the registry confirmed within the
 * last {@link #LINK_MAX_AGE} is admitted on the stored link when the registry cannot be asked,
 * provided the name from the provider matches the stored one.
 */
public class InboundFederationHandler
    implements RequestHandler<InboundFederationEvent, InboundFederationEvent> {

  /** Matched by a metric filter in the template, so the start of the message must stay. */
  public static final String LINK_REWRITTEN_MESSAGE = "Link rewritten for user {} from {} to {}";

  /** Matched by a metric filter in the template, so the start of the message must stay. */
  public static final String ADMITTED_ON_LINK_MESSAGE =
      "Admitted on link for user {} as {}, verified {}, because the person registry failed";

  public static final Duration LINK_MAX_AGE = Duration.ofDays(30);

  private static final Logger LOGGER = LoggerFactory.getLogger(InboundFederationHandler.class);
  private static final String DONE_MESSAGE =
      "Inbound federation for user {} resolved to {} through the {} in {} ms";
  private static final String FAILED_MESSAGE = "Inbound federation for user {} failed";
  private static final String NO_NIN_MESSAGE =
      "No national identity number from {} for user {}, keys in idToken {} and userInfo {}";
  private static final String NO_NAME_MESSAGE =
      "No name from {} for user {}, keys in idToken {} and userInfo {}";
  private static final String NAME_MATCH_MESSAGE =
      "Name from {} for user {} matches the registry: {}";
  private static final String NOT_ADMITTED_MESSAGE = "User {} not admitted on link: {}";
  private static final String PERSON_EXISTED_MESSAGE =
      "Person existed in the registry when creating it, fetching it instead";
  private static final String REGISTRY = "person registry";
  private static final String LINK = "link";
  private static final String NO_LINK = "no link";
  private static final String LINK_TOO_OLD = "link older than " + LINK_MAX_AGE.toDays() + " days";
  private static final String NAME_MISMATCH = "name does not match the linked person";

  private final CustomerService customerService;
  private final IdentityService identityService;
  private final UserEntriesCreatorForPerson userCreator;
  private final PersonRegistry personRegistry;
  private final Clock clock;

  @JacocoGenerated
  public InboundFederationHandler() {
    this(
        defaultCustomerService(DEFAULT_DYNAMO_CLIENT),
        defaultIdentityService(DEFAULT_DYNAMO_CLIENT),
        CristinPersonRegistry.defaultPersonRegistry(),
        Clock.systemUTC());
  }

  public InboundFederationHandler(
      CustomerService customerService,
      IdentityService identityService,
      PersonRegistry personRegistry,
      Clock clock) {
    this.customerService = customerService;
    this.identityService = identityService;
    this.userCreator = new UserEntriesCreatorForPerson(identityService);
    this.personRegistry = personRegistry;
    this.clock = clock;
  }

  @Override
  @SuppressWarnings("PMD.AvoidCatchingGenericException") // every failure gets a known error code
  public InboundFederationEvent handleRequest(InboundFederationEvent event, Context context) {
    var start = clock.instant();
    try {
      var login = FederatedLogin.from(event);
      var admitted = admit(login);
      LOGGER.info(
          DONE_MESSAGE,
          login.username(),
          admitted.cristinId(),
          admitted.path(),
          Duration.between(start, clock.instant()).toMillis());
    } catch (RuntimeException e) {
      throw asKnownFailure(event.getUserName(), e);
    }
    event.getResponse().setUserAttributesToMap(new HashMap<>());
    return event;
  }

  private static RuntimeException asKnownFailure(String userName, RuntimeException failure) {
    if (failure instanceof IdentityServiceMissingNinException
        || failure instanceof IdentityServiceLoginNotCompletedException) {
      return failure;
    }
    LOGGER.error(FAILED_MESSAGE, userName, failure);
    return new IdentityServiceLoginNotCompletedException(failure);
  }

  /**
   * Only a failure of the lookup itself leads to the stored link. Once the registry has answered
   * that it does not know the number, a failure to create the person fails this login; it must not
   * admit the user as the person the link points to.
   */
  private Admitted admit(FederatedLogin login) {
    var nin = Optional.ofNullable(login.nin()).orElseThrow(() -> missingNin(login));
    final Optional<Person> found;
    try {
      found = personRegistry.fetchPersonByNin(nin);
    } catch (IdentityServiceException e) {
      return admitOnLink(login, e);
    }
    var person =
        found
            .or(() -> createPerson(personRegistry, login, nin))
            .orElseThrow(IdentityServiceLoginNotCompletedException::new);
    var registryAnswered = clock.instant();
    // TODO: Measures how often the name check behind admitOnLink would pass. Remove in NP-51975.
    LOGGER.info(
        NAME_MATCH_MESSAGE,
        login.providerName(),
        login.username(),
        login.hasName(person.getFirstname(), person.getSurname()));
    var customers = ActiveCustomers.forPerson(person, customerService);
    userCreator.createUsers(new UserCreationContext(person, customers, login.feideIdentifier()));
    writeLinkAndPersonRows(login, person, customers, registryAnswered);
    return new Admitted(person.getId(), REGISTRY);
  }

  /** A person that another login created in the meantime answers 409 and is fetched instead. */
  private static Optional<Person> createPerson(
      PersonRegistry registry, FederatedLogin login, NationalIdentityNumber nin) {
    var name = Optional.ofNullable(login.name()).orElseThrow(() -> noName(login));
    try {
      return registry.createPerson(nin, name.givenName(), name.familyName());
    } catch (IdentityServiceAlreadyExistsException e) {
      LOGGER.info(PERSON_EXISTED_MESSAGE);
      return registry.fetchPersonByNin(nin);
    }
  }

  /**
   * When the registry cannot be asked, a Cognito user it confirmed recently is admitted on the
   * stored link, so that an outage does not lock known users out. The age is that of the link,
   * which records when this Cognito user was last confirmed; the person row is shared by all the
   * person's users. The name check keeps a provider account that changed hands during the outage
   * from being admitted as its previous holder. The rows are left as they are; the next login that
   * reaches the registry writes them again.
   */
  private Admitted admitOnLink(FederatedLogin login, IdentityServiceException registryFailure) {
    var link = identityService.fetchLink(login.username());
    var linkedPerson = link.flatMap(found -> identityService.fetchLinkedPerson(found.cristinId()));
    if (link.isEmpty() || linkedPerson.isEmpty()) {
      throw notAdmitted(login, NO_LINK, registryFailure);
    }
    var verified = link.get().modified();
    if (verified.isBefore(clock.instant().minus(LINK_MAX_AGE))) {
      throw notAdmitted(login, LINK_TOO_OLD, registryFailure);
    }
    var person = linkedPerson.get();
    if (!login.hasName(person.givenName(), person.familyName())) {
      throw notAdmitted(login, NAME_MISMATCH, registryFailure);
    }
    LOGGER.warn(
        ADMITTED_ON_LINK_MESSAGE, login.username(), person.cristinId(), verified, registryFailure);
    return new Admitted(person.cristinId(), LINK);
  }

  private static IdentityServiceLoginNotCompletedException notAdmitted(
      FederatedLogin login, String reason, IdentityServiceException registryFailure) {
    LOGGER.error(NOT_ADMITTED_MESSAGE, login.username(), reason, registryFailure);
    return new IdentityServiceLoginNotCompletedException(registryFailure);
  }

  private static IdentityServiceMissingNinException missingNin(FederatedLogin login) {
    LOGGER.warn(
        NO_NIN_MESSAGE,
        login.providerName(),
        login.username(),
        login.idTokenKeys(),
        login.userInfoKeys());
    return new IdentityServiceMissingNinException();
  }

  private static IdentityServiceLoginNotCompletedException noName(FederatedLogin login) {
    LOGGER.error(
        NO_NAME_MESSAGE,
        login.providerName(),
        login.username(),
        login.idTokenKeys(),
        login.userInfoKeys());
    return new IdentityServiceLoginNotCompletedException();
  }

  /**
   * Remembers what the registry answered, for the pre-token Lambda. Unlike there, the rows are not
   * best effort here: without the link the pre-token Lambda has nothing to resolve the user from.
   */
  private void writeLinkAndPersonRows(
      FederatedLogin login, Person person, Set<CustomerDto> customers, Instant registryAnswered) {
    var previous = identityService.fetchLink(login.username());
    var customerIds = customers.stream().map(CustomerDto::getId).collect(Collectors.toSet());
    identityService.saveLinkedPerson(
        new LinkedPerson(
            person.getId(),
            person.getFirstname(),
            person.getSurname(),
            customerIds,
            registryAnswered));
    identityService.saveLink(
        new IdentityLink(
            login.username(),
            person.getId(),
            login.feideIdentifier(),
            login.orgFeideDomain(),
            registryAnswered));
    previous
        .filter(link -> !link.cristinId().equals(person.getId()))
        .ifPresent(
            link ->
                LOGGER.warn(
                    LINK_REWRITTEN_MESSAGE, login.username(), link.cristinId(), person.getId()));
  }

  private record Admitted(URI cristinId, String path) {}
}
