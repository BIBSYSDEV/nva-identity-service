package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber.fromString;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class CreateUserHandler extends HandlerWithEventualConsistency<CreateUserRequest, UserDto> {

    private static final String BAD_GATEWAY_ERROR_MESSAGE = "Something went wrong, contact application administrator!";
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserHandler.class);
    private final UserEntriesCreatorForPerson userCreator;
    private final IdentityService identityService;
    private final CustomerService customerService;
    private final PersonRegistry personRegistry;
    private int statusCode;

    @JacocoGenerated
    public CreateUserHandler() {
        this(IdentityService.defaultIdentityService(),
                defaultCustomerService(),
                CristinPersonRegistry.defaultPersonRegistry(), new Environment());
    }

    public CreateUserHandler(IdentityService identityService,
                             CustomerService customerService,
                             PersonRegistry personRegistry, Environment environment) {
        super(CreateUserRequest.class, environment);
        this.userCreator = defaultUserCreator(identityService);
        this.identityService = identityService;
        this.customerService = customerService;
        this.personRegistry = personRegistry;
    }

    private static UserEntriesCreatorForPerson defaultUserCreator(IdentityService identityService) {
        return new UserEntriesCreatorForPerson(identityService);
    }

    @Override
    protected void validateRequest(CreateUserRequest createUserRequest, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        authorize(createUserRequest, requestInfo);
    }

    @Override
    protected UserDto processInput(CreateUserRequest input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {

        var existingUser = fetchExistingUser(input);
        if (existingUser.isPresent()) {
            this.statusCode = HTTP_OK;
            return existingUser.get();
        } else {
            this.statusCode = HTTP_CREATED;
            return persistNewUser(input);
        }
    }

    private UserDto persistNewUser(CreateUserRequest input) throws ApiGatewayException {
        return attempt(() -> createNewUser(input))
                .map(userDto -> addRolesAndViewingScope(userDto, input))
                .map(identityService::updateUser)
                .orElseThrow(CreateUserHandler::castToCorrectRuntimeException);
    }

    public static ApiGatewayException castToCorrectRuntimeException(Failure<?> failure) {
        var exception = failure.getException();
        if (exception instanceof ConflictException) {
            return (ConflictException) exception;
        } else {
            final Throwable cause = failure.getException();
            LOGGER.error("Failed to create or update user!", cause);
            return new BadGatewayException(BAD_GATEWAY_ERROR_MESSAGE);
        }
    }

    private UserDto createNewUser(CreateUserRequest input) throws ConflictException, BadGatewayException {
        var person = fetchCristinPersonFromIdentifierOrNin(input).orElseThrow();

        var customersWithActiveAffiliations = fetchCustomersWithActiveAffiliations(
                person.getAffiliations());

        explainWhyUserCannotBeCreated(person, customersWithActiveAffiliations);

        var customers = Collections.singleton(
                customersWithActiveAffiliations.stream()
                        .filter(customerByIdOrCristinId(input))
                        .collect(SingletonCollector.collect())
        );
        var context = new UserCreationContext(person, customers);
        var users = userCreator.createUsers(context);

        return users.stream().collect(SingletonCollector.collect());
    }

    private Predicate<CustomerDto> customerByIdOrCristinId(CreateUserRequest input) {
        return c -> c.getId().equals(input.customerId()) || c.getCristinId().equals(input.customerId());
    }

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(List<Affiliation> affiliations) {
        return affiliations.stream()
                .map(Affiliation::getInstitutionId)
                .map(attempt(customerService::getCustomerByCristinId))
                .flatMap(Try::stream)
                .collect(Collectors.toSet());
    }

    private void explainWhyUserCannotBeCreated(Person person,
                                               Set<CustomerDto> customers) throws ConflictException {

        if (isEmptyCollection(person.getAffiliations())) {
            throw new ConflictException(noActiveAffiliationsMessage(person.getIdentifier()));
        }
        if (isEmptyCollection(customers)) {
            throw new ConflictException(noCustomersForActiveAffiliations(person.getIdentifier()));
        }
    }

    private String noCustomersForActiveAffiliations(String personId) {
        return String.format("Person %s has no active affiliations with an Institution that is an NVA customer",
                personId);
    }

    private String noActiveAffiliationsMessage(String personId) {
        return String.format("Person %s has no active affiliations with an institution", personId);
    }

    private <T> boolean isEmptyCollection(Collection<T> collection) {
        return isNull(collection) || collection.isEmpty();
    }

    private UserDto addRolesAndViewingScope(UserDto newUser, CreateUserRequest input) {
        return getEventuallyConsistent(() -> identityService.getUser(newUser))
                .map(UserDto::copy)
                .map(user -> user.withRoles(createUnionOfRoleSets(input, newUser)))
                .map(user -> user.withViewingScope(input.viewingScope()))
                .map(UserDto.Builder::build)
                .orElseThrow();
    }

    private Set<RoleDto> createUnionOfRoleSets(CreateUserRequest input, UserDto newUser) {
        return Stream.of(input.roles(), newUser.getRoles())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Optional<UserDto> fetchExistingUser(CreateUserRequest input) throws ConflictException, BadGatewayException {
        var cristinPerson = fetchCristinPersonFromIdentifierOrNin(input);
        if (cristinPerson.isPresent()) {
            return fetchUserAtCustomer(cristinPerson.get(), input.customerId());
        } else {
            throw new ConflictException(personIsNotRegisteredError(input.nationalIdentityNumber()));
        }
    }

    private Optional<Person> fetchCristinPersonFromIdentifierOrNin(CreateUserRequest input) throws BadGatewayException {
        try {
            return Optional.ofNullable(input.cristinIdentifier())
                    .map(personRegistry::fetchPersonByIdentifier)
                    .orElseGet(() -> personRegistry.fetchPersonByNin(fromString(input.nationalIdentityNumber())));
        } catch (Exception e) {
            throw new BadGatewayException(BAD_GATEWAY_ERROR_MESSAGE);
        }
    }

    private Optional<UserDto> fetchUserAtCustomer(Person person, URI customerId) throws BadGatewayException {
        try {
            return identityService.getUsersByCristinId(person.getId()).stream()
                    .filter(userDto -> userDto.getInstitution().equals(customerId))
                    .findFirst();
        } catch (Exception e) {
            throw new BadGatewayException(BAD_GATEWAY_ERROR_MESSAGE);
        }
    }

    private String personIsNotRegisteredError(String nin) {
        return String.format("Person %s is not registered in the Person Registry", nin);
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUserRequest input, UserDto output) {
        return statusCode;
    }

    private void authorize(CreateUserRequest input, RequestInfo requestInfo) throws ForbiddenException {
        if (userIsInternalBackendOrHasManageOwnAffiliationAccess(requestInfo)) {
            return;
        }

        var roles = input.roles().stream().map(RoleDto::getRoleName).collect(Collectors.toSet());

        if (roles.contains(RoleName.APPLICATION_ADMIN) || !requestInfo.userIsAuthorized(MANAGE_OWN_AFFILIATION)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsInternalBackendOrHasManageOwnAffiliationAccess(RequestInfo requestInfo) {
        return requestInfo.clientIsInternalBackend()
                || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS);
    }
}
