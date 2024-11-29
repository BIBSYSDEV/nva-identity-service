package no.unit.nva.useraccessservice.usercreation;

import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.person.Person;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static nva.commons.core.attempt.Try.attempt;

public class UserEntriesCreatorForPerson {
    public static final RoleDto ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION =
        RoleDto.newBuilder().withRoleName(RoleName.CREATOR).build();
    private static final String AT = "@";
    private final IdentityService identityService;

    public UserEntriesCreatorForPerson(IdentityService identityService) {
        this.identityService = identityService;
    }

    public List<UserDto> createUsers(UserCreationContext context) {
        return createOrFetchUserEntriesForPerson(context);
    }

    private List<UserDto> createOrFetchUserEntriesForPerson(UserCreationContext context) {
        return context.getCustomers().stream()
            .map(customer -> createNewUserObject(customer, context))
            .map(user -> getExistingUserOrCreateNew(user, context))
            .collect(Collectors.toList());
    }

    private UserDto createNewUserObject(CustomerDto customer, UserCreationContext context) {
        var person = context.getPerson();
        var affiliation = person.getConsistentUnitAffiliation(customer.getCristinId());
        var feideIdentifier = context.getFeideIdentifier();
        var personId = context.getPerson().getId();
        var customerCristinId = customer.getCristinId();
        var username = createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(person.getIdentifier(),
            customerCristinId);
        var user = UserDto.newBuilder()
            .withUsername(username)
            .withRoles(Collections.singletonList(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
            .withFeideIdentifier(feideIdentifier)
            .withInstitution(customer.getId())
            .withGivenName(person.getFirstname())
            .withFamilyName(person.getSurname())
            .withCristinId(personId)
            .withInstitutionCristinId(customer.getCristinId())
            .withAffiliation(affiliation);

        return user.build();
    }

    // Create a username that will allow the user to access their resources even if the identity service stack
    // gets totally destroyed.
    private String createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(String personIdentifier,
                                                                                   URI customerCristinId) {

        var customerIdentifier = UriWrapper.fromUri(customerCristinId).getLastPathElement();
        return personIdentifier + AT + customerIdentifier;
    }

    private UserDto getExistingUserOrCreateNew(UserDto user, UserCreationContext context) {
        return attempt(() -> fetchUserBasedOnCristinIdentifiers(user, context.getPerson()))
            .or(() -> fetchLegacyUserWithFeideIdentifier(user, context))
            .or(() -> addUser(user))
            .orElseThrow();
    }

    private UserDto fetchLegacyUserWithFeideIdentifier(UserDto userWithUpdatedInformation,
                                                       UserCreationContext context) throws NotFoundException {
        var queryObject =
            UserDto.newBuilder().withUsername(context.getFeideIdentifier()).build();
        var savedUser = identityService.getUser(queryObject);
        var affiliation = context.getPerson()
            .getConsistentUnitAffiliation(userWithUpdatedInformation.getInstitutionCristinId());
        var updatedUser = savedUser.copy()
            .withFeideIdentifier(userWithUpdatedInformation.getFeideIdentifier())
            .withCristinId(userWithUpdatedInformation.getCristinId())
            .withInstitutionCristinId(userWithUpdatedInformation.getInstitutionCristinId())
            .withAffiliation(affiliation)
            .build();
        identityService.updateUser(updatedUser);

        return updatedUser;
    }

    private UserDto fetchUserBasedOnCristinIdentifiers(UserDto user, Person person) throws NotFoundException {
        var existingUser =
            identityService.getUserByPersonCristinIdAndCustomerCristinId(user.getCristinId(),
                user.getInstitutionCristinId());
        return updateUserAffiliation(existingUser, person);
    }

    private UserDto updateUserAffiliation(UserDto existingUser,
                                          Person person) throws NotFoundException {
        var affiliation = person.getConsistentUnitAffiliation(existingUser.getInstitutionCristinId());
        var updatedUser = existingUser.copy().withAffiliation(affiliation).build();
        return identityService.updateUser(updatedUser);
    }

    private UserDto addUser(UserDto user) throws ConflictException {
        identityService.addUser(user);
        return user;
    }
}
