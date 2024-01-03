
package no.unit.nva.handlers;

import static java.util.Objects.isNull;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserCreationContext;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.person.Affiliation;
import no.unit.nva.useraccessservice.usercreation.person.NationalIdentityNumber;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;

public class CreateUserHandler extends HandlerWithEventualConsistency<CreateUserRequest, UserDto> {

    private final UserEntriesCreatorForPerson userCreator;
    private final IdentityService identityService;
    private final CustomerService customerService;
    private final PersonRegistry personRegistry;

    @JacocoGenerated
    public CreateUserHandler() {
        this(IdentityService.defaultIdentityService(), defaultCustomerService());
    }

    @JacocoGenerated
    public CreateUserHandler(IdentityService identityService, CustomerService customerService) {
        this(defaultUserCreator(identityService),
             identityService,
             customerService,
             CristinPersonRegistry.defaultPersonRegistry());
    }

    public CreateUserHandler(UserEntriesCreatorForPerson userCreator,
                             IdentityService identityService,
                             CustomerService customerService,
                             PersonRegistry personRegistry) {
        super(CreateUserRequest.class);
        this.userCreator = userCreator;
        this.identityService = identityService;
        this.customerService = customerService;
        this.personRegistry = personRegistry;
    }

    @Override
    protected UserDto processInput(CreateUserRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        authorize(requestInfo);

        var newUser = createNewUser(input);

        var userWithUpdatedRoles = addRolesToCreatedUser(input, newUser);
        identityService.updateUser(userWithUpdatedRoles);
        return userWithUpdatedRoles;
    }

    @Override
    protected Integer getSuccessStatusCode(CreateUserRequest input, UserDto output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static UserEntriesCreatorForPerson defaultUserCreator(IdentityService identityService) {
        return new UserEntriesCreatorForPerson(identityService);
    }

    private UserDto createNewUser(CreateUserRequest input) throws ConflictException {
        var nin = new NationalIdentityNumber(input.getNin());
        var person = personRegistry.fetchPersonByNin(nin)
                         .orElseThrow(() -> new ConflictException(personIsNotRegisteredError(input.getNin())));

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
        return c -> c.getId().equals(input.getCustomerId()) || c.getCristinId().equals(input.getCustomerId());
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

    private String personIsNotRegisteredError(String nin) {
        return String.format("Person %s is not registered in the Person Registry", nin);
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

    private UserDto addRolesToCreatedUser(CreateUserRequest input, UserDto newUser) {
        var allRoles = createUnionOfRoleSets(input, newUser);
        return getEventuallyConsistent(() -> identityService.getUser(newUser))
                   .map(UserDto::copy)
                   .map(user -> user.withRoles(allRoles))
                   .map(UserDto.Builder::build)
                   .orElseThrow();
    }

    private Set<RoleDto> createUnionOfRoleSets(CreateUserRequest input, UserDto newUser) {
        return Stream.of(input.getRoles(), newUser.getRoles())
                   .flatMap(Collection::stream)
                   .collect(Collectors.toSet());
    }

    private void authorize(RequestInfo requestInfo) throws ForbiddenException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !(requestInfo.clientIsInternalBackend()
                 || requestInfo.userIsAuthorized(MANAGE_OWN_AFFILIATION)
                 || requestInfo.userIsAuthorized(MANAGE_CUSTOMERS));
    }
}
