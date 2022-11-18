package no.unit.nva.useraccessservice.usercreation;

import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserEntriesCreatorForPerson {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserEntriesCreatorForPerson.class);
    public static final RoleDto ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION =
        RoleDto.newBuilder().withRoleName(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT).build();
    private static final String AT = "@";
    private final IdentityService identityService;

    public UserEntriesCreatorForPerson(IdentityService identityService) {
        this.identityService = identityService;
    }

    public List<UserDto> createUsers(PersonInformation personInformation, Set<CustomerDto> customers) {

        return createOrFetchUserEntriesForPerson(personInformation, customers, keepAll());
    }

    public List<UserDto> createUser(PersonInformation personInformation,
                                    Set<CustomerDto> customers, URI selectedCustomer) {
        return createOrFetchUserEntriesForPerson(personInformation, customers,
                                                 customerDto -> isSelectedCustomer(customerDto, selectedCustomer));
    }

    private boolean isSelectedCustomer(CustomerDto customerDto, URI selectedCustomerId) {
        LOGGER.info("Checking for selected customer: {} {} {}", customerDto.getId(), customerDto.getCristinId(),
                    selectedCustomerId);
        return customerDto.getId().equals(selectedCustomerId) || customerDto.getCristinId().equals(selectedCustomerId);
    }

    private Predicate<CustomerDto> keepAll() {
        return customerDto -> true;
    }

    private List<UserDto> createOrFetchUserEntriesForPerson(PersonInformation personInformation,
                                                            Set<CustomerDto> customers,
                                                            Predicate<CustomerDto> customerFilter) {
        LOGGER.info("Customers: {}", customers);

        return customers.stream()
                   .filter(customerFilter)
                   .map(customer -> createNewUserObject(customer, personInformation))
                   .map(user -> getExistingUserOrCreateNew(user, personInformation))
                   .collect(Collectors.toList());
    }

    private UserDto createNewUserObject(CustomerDto customer, PersonInformation personInformation) {
        LOGGER.info("Creating new user object for customer {}", customer.getId());

        var affiliation = personInformation.getOrganizationAffiliation(customer.getCristinId());
        var feideIdentifier = personInformation.getFeideIdentifier();
        var personRegistryId = personInformation.getPersonRegistryId().orElseThrow();
        var user = UserDto.newBuilder()
                       .withUsername(
                           createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(personRegistryId, customer))
                       .withRoles(Collections.singletonList(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
                       .withFeideIdentifier(feideIdentifier)
                       .withInstitution(customer.getId())
                       .withGivenName(personInformation.getGivenName().orElseThrow())
                       .withFamilyName(personInformation.getFamilyName().orElseThrow())
                       .withCristinId(personRegistryId)
                       .withInstitutionCristinId(customer.getCristinId())
                       .withAffiliation(affiliation);

        LOGGER.info("New user object: {}", user);
        return user.build();
    }

    // Create a username that will allow the user to access their resources even if the identity service stack
    // gets totally destroyed.
    private String createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(
        URI personRegistryId,
        CustomerDto customer) {

        var customerIdentifier = UriWrapper.fromUri(customer.getCristinId()).getLastPathElement();
        var personIdentifier = UriWrapper.fromUri(personRegistryId).getLastPathElement();
        return personIdentifier + AT + customerIdentifier;
    }

    private UserDto getExistingUserOrCreateNew(UserDto user, PersonInformation personInformation) {
        return attempt(() -> fetchUserBasedOnCristinIdentifiers(user, personInformation))
                   .or(() -> fetchLegacyUserWithFeideIdentifier(user, personInformation))
                   .or(() -> addUser(user))
                   .orElseThrow();
    }

    private UserDto fetchLegacyUserWithFeideIdentifier(UserDto userWithUpdatedInformation,
                                                       PersonInformation personInformation) throws NotFoundException {
        var queryObject =
            UserDto.newBuilder().withUsername(personInformation.getFeideIdentifier()).build();
        var savedUser = identityService.getUser(queryObject);
        var affiliation =
            personInformation.getOrganizationAffiliation(userWithUpdatedInformation.getInstitutionCristinId());
        var updatedUser = savedUser.copy()
                              .withFeideIdentifier(userWithUpdatedInformation.getFeideIdentifier())
                              .withCristinId(userWithUpdatedInformation.getCristinId())
                              .withInstitutionCristinId(userWithUpdatedInformation.getInstitutionCristinId())
                              .withAffiliation(affiliation)
                              .build();
        identityService.updateUser(updatedUser);
        LOGGER.info("Updated legacy user with feide identifier username!");

        return updatedUser;
    }

    private UserDto fetchUserBasedOnCristinIdentifiers(UserDto user, PersonInformation personInformation)
        throws NotFoundException {
        var existingUser =
            identityService.getUserByPersonCristinIdAndCustomerCristinId(user.getCristinId(),
                                                                         user.getInstitutionCristinId());
        return updateUserAffiliation(existingUser, personInformation);
    }

    private UserDto updateUserAffiliation(UserDto existingUser,
                                          PersonInformation personInformation) throws NotFoundException {
        var affiliation = personInformation.getOrganizationAffiliation(existingUser.getInstitutionCristinId());
        var updatedUser = existingUser.copy().withAffiliation(affiliation).build();
        identityService.updateUser(updatedUser);
        LOGGER.info("Updated user!");
        return updatedUser;
    }

    private UserDto addUser(UserDto user) throws ConflictException {
        identityService.addUser(user);
        LOGGER.info("Added user!");
        return user;
    }
}
