
package no.unit.nva.handlers;

import static java.util.Objects.isNull;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.PersonInformation;
import no.unit.nva.useraccessservice.usercreation.PersonInformationImpl;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.PersonAffiliation;
import no.unit.nva.useraccessservice.usercreation.person.PersonRegistry;
import no.unit.nva.useraccessservice.usercreation.person.cristin.CristinPersonRegistry;
import nva.commons.apigateway.AccessRight;
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
        var personInformation = new PersonInformationImpl(personRegistry,
                                                          input.getNin());
        var customers = fetchCustomersWithActiveAffiliations(
            personInformation.getPersonAffiliations());

        explainWhyUserCannotBeCreated(personInformation, input.getNin(), customers);

        var users = userCreator.createUser(personInformation, customers, input.getCustomerId());

        return users.stream().collect(SingletonCollector.collect());
    }

    private Set<CustomerDto> fetchCustomersWithActiveAffiliations(List<PersonAffiliation> personAffiliations) {
        return personAffiliations.stream()
            .map(PersonAffiliation::getInstitutionCristinId)
            .map(attempt(customerService::getCustomerByCristinId))
            .flatMap(Try::stream)
            .collect(Collectors.toSet());
    }

    private void explainWhyUserCannotBeCreated(PersonInformation personInformation,
                                               String nin,
                                               Set<CustomerDto> customers)
        throws ConflictException {
        if (!personInformation.personIsPresentInPersonRegistry()) {
            throw new ConflictException(personIsNotRegisteredError(nin));
        }
        if (isEmptyCollection(personInformation.getPersonAffiliations())) {
            throw new ConflictException(noActiveAffiliationsMessage(personInformation, nin));
        }
        if (isEmptyCollection(customers)) {
            throw new ConflictException(noCustomersForActiveAffiliations(personInformation, nin));
        }
    }

    private String personIsNotRegisteredError(String nin) {
        return String.format("Person %s is not registered in the Person Registry", nin);
    }

    private String noCustomersForActiveAffiliations(PersonInformation personInformation, String nin) {
        var personId = personInformation.getPersonRegistryId()
                           .map(URI::toString)
                           .orElse(nin);

        return String.format("Person %s has no active affiliations with an Institution that is an NVA customer",
                             personId);
    }

    private String noActiveAffiliationsMessage(PersonInformation personInformation, String nin) {
        var personId = personInformation.getPersonRegistryId()
                           .map(URI::toString)
                           .orElse(nin);

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
        return !(
            requestInfo.clientIsInternalBackend()
            || requestInfo.userIsAuthorized(AccessRight.EDIT_OWN_INSTITUTION_USERS.toString())
            || requestInfo.userIsApplicationAdmin()

        );
    }
}
