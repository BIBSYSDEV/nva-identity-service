package no.unit.nva.handlers;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.models.CreateUserRequest;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.UserEntriesCreatorForPerson;
import no.unit.nva.useraccessservice.usercreation.cristin.person.CristinClient;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;

public class CreateUserHandler extends HandlerWithEventualConsistency<CreateUserRequest, UserDto> {

    private final UserEntriesCreatorForPerson userCreator;
    private final IdentityService identityService;

    @JacocoGenerated
    public CreateUserHandler(IdentityService identityService) {
        this(defaultUserCreator(identityService), identityService);
    }

    public CreateUserHandler(UserEntriesCreatorForPerson userCreator,
                             IdentityService identityService) {
        super(CreateUserRequest.class);
        this.userCreator = userCreator;
        this.identityService = identityService;
    }

    @Override
    protected UserDto processInput(CreateUserRequest input, RequestInfo requestInfo, Context context)
        throws ForbiddenException, NotFoundException {
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
        return new UserEntriesCreatorForPerson(defaultCustomerService(),
                                               CristinClient.defaultClient(),
                                               identityService);
    }

    private UserDto createNewUser(CreateUserRequest input) {
        var personInformation = userCreator.collectPersonInformation(input.getNin());
        return userCreator.createUser(personInformation, input.getCustomerId())
            .stream()
            .collect(SingletonCollector.collect());
    }

    private UserDto addRolesToCreatedUser(CreateUserRequest input, UserDto newUser) {
        return getEventuallyConsistent(() -> identityService.getUser(newUser))
            .map(UserDto::copy)
            .map(user -> user.withRoles(input.getRoles()))
            .map(UserDto.Builder::build)
            .orElseThrow();
    }

    private void authorize(RequestInfo requestInfo) throws ForbiddenException {
        if (userIsNotAuthorized(requestInfo)) {
            throw new ForbiddenException();
        }
    }

    private boolean userIsNotAuthorized(RequestInfo requestInfo) {
        return !(
            requestInfo.userIsAuthorized(AccessRight.EDIT_OWN_INSTITUTION_USERS.toString())
            || requestInfo.isApplicationAdmin()
        );
    }
}
