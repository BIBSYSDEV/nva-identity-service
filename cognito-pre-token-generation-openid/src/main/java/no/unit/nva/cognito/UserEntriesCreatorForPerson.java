package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.AT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinClient;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;

public class UserEntriesCreatorForPerson {

    private static final RoleDto ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION =
        RoleDto.newBuilder().withRoleName("Creator").build();

    private final CustomerService customerService;
    private final CristinClient cristinClient;
    private final BackendJwtTokenRetriever backendJwtTokenRetriever;
    private final IdentityService identityService;

    public UserEntriesCreatorForPerson(CustomerService customerService,
                                       CristinClient cristinClient,
                                       BackendJwtTokenRetriever backendJwtTokenRetriever,
                                       IdentityService identityService) {
        this.customerService = customerService;
        this.cristinClient = cristinClient;
        this.backendJwtTokenRetriever = backendJwtTokenRetriever;
        this.identityService = identityService;
    }

    public List<UserDto> createUsers(AuthenticationInformation authenticationInfo) {
        createUserRole();
        return createOrFetchUserEntriesForPerson(authenticationInfo);
    }

    public void createUserRole() {
        try {
            identityService.addRole(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION);
        } catch (Exception ignored) {
            //Do nothing if role exists.
        }
    }

    public List<UserDto> createOrFetchUserEntriesForPerson(AuthenticationInformation authenticationInformation) {

        return authenticationInformation.getActiveCustomers().stream()
            .map(customer -> createNewUserObject(customer, authenticationInformation))
            .map(user -> getExistingUserOrCreateNew(user, authenticationInformation))
            .collect(Collectors.toList());
    }

    public AuthenticationInformation collectInformationForPerson(CognitoUserPoolPreTokenGenerationEvent input) {
        var authenticationInfo = AuthenticationInformation.create(input);

        var cristinResponse =
            fetchPersonInformationFromCristin(input, authenticationInfo.getNationalIdentityNumber());
        authenticationInfo.setCristinResponse(cristinResponse);

        var affiliationInformation = fetchParentInstitutionsForPersonAffiliations(authenticationInfo);
        authenticationInfo.setPersonAffiliations(affiliationInformation);

        var activeCustomers = fetchCustomersForActiveAffiliations(authenticationInfo);
        authenticationInfo.setActiveCustomers(activeCustomers);

        return authenticationInfo;
    }

    private Set<CustomerDto> fetchCustomersForActiveAffiliations(AuthenticationInformation authenticationInformation) {

        return authenticationInformation.getPersonAffiliations()
            .stream()
            .map(PersonAffiliation::getParentInstitution)
            .map(attempt(customerService::getCustomerByCristinId))
            .flatMap(Try::stream)
            .collect(Collectors.toSet());
    }

    private List<PersonAffiliation> fetchParentInstitutionsForPersonAffiliations(
        AuthenticationInformation authenticationInformation) {
        return authenticationInformation.getCristinPersonResponse().getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(this::fetchParentInstitutionCristinId)
            .collect(Collectors.toList());
    }

    private PersonAffiliation fetchParentInstitutionCristinId(URI mostSpecificAffiliation) {
        return attempt(() -> cristinClient.fetchTopLevelOrgUri(mostSpecificAffiliation))
            .map(parentInstitution -> PersonAffiliation.create(mostSpecificAffiliation, parentInstitution))
            .orElseThrow();
    }

    private CristinPersonResponse fetchPersonInformationFromCristin(
        CognitoUserPoolPreTokenGenerationEvent input, String nin) {
        var jwtToken = backendJwtTokenRetriever.fetchJwtToken(input.getUserPoolId());
        return attempt(() -> cristinClient.sendRequestToCristin(jwtToken, nin)).orElseThrow();
    }

    private UserDto createNewUserObject(CustomerDto customer,
                                        AuthenticationInformation authenticationInformation) {

        var cristinResponse = authenticationInformation.getCristinPersonResponse();
        var affiliation = authenticationInformation.getOrganizationAffiliation(customer.getCristinId());
        var feideIdentifier = authenticationInformation.getFeideIdentifier();
        var user = UserDto.newBuilder()
            .withUsername(createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(cristinResponse, customer))
            .withRoles(Collections.singletonList(ROLE_FOR_PEOPLE_WITH_ACTIVE_AFFILIATION))
            .withFeideIdentifier(feideIdentifier)
            .withInstitution(customer.getId())
            .withGivenName(cristinResponse.extractFirstName())
            .withFamilyName(cristinResponse.extractLastName())
            .withCristinId(cristinResponse.getCristinId())
            .withCristinId(cristinResponse.getCristinId())
            .withInstitutionCristinId(customer.getCristinId())
            .withAffiliation(affiliation);

        return user.build();
    }

    // Create a username that will allow the user to access their resources even if the identity service stack
    // gets totally destroyed.
    private String createConsistentUsernameBasedOnPersonIdentifierAndOrgIdentifier(
        CristinPersonResponse cristinResponse,
        CustomerDto customer) {
        var personIdentifier = cristinResponse.getPersonsCristinIdentifier().getValue();
        var customerIdentifier = UriWrapper.fromUri(customer.getCristinId()).getLastPathElement();
        return personIdentifier + AT + customerIdentifier;
    }

    private UserDto getExistingUserOrCreateNew(UserDto user, AuthenticationInformation authenticationInformation) {
        return attempt(() -> fetchUserBasedOnCristinIdentifiers(user, authenticationInformation))
            .or(() -> fetchLegacyUserWithFeideIdentifier(user, authenticationInformation))
            .or(() -> addUser(user))
            .orElseThrow();
    }

    private UserDto fetchLegacyUserWithFeideIdentifier(UserDto userWithUpdatedInformation,
                                                       AuthenticationInformation authenticationInformation) {
        var queryObject =
            UserDto.newBuilder().withUsername(authenticationInformation.getFeideIdentifier()).build();
        var savedUser = identityService.getUser(queryObject);
        var affiliation =
            authenticationInformation.getOrganizationAffiliation(userWithUpdatedInformation.getInstitutionCristinId());
        var updatedUser = savedUser.copy()
            .withFeideIdentifier(userWithUpdatedInformation.getFeideIdentifier())
            .withCristinId(userWithUpdatedInformation.getCristinId())
            .withInstitutionCristinId(userWithUpdatedInformation.getInstitutionCristinId())
            .withAffiliation(affiliation)
            .build();
        identityService.updateUser(updatedUser);
        return updatedUser;
    }

    private UserDto fetchUserBasedOnCristinIdentifiers(UserDto user,
                                                       AuthenticationInformation authenticationInformation) {
        var existingUser =
            identityService.getUserByPersonCristinIdAndCustomerCristinId(user.getCristinId(), user.getInstitutionCristinId());
        return updateUserAffiliation(user, authenticationInformation, existingUser);
    }

    private UserDto updateUserAffiliation(UserDto user, AuthenticationInformation authenticationInformation,
                                          UserDto existingUser) {
        var affiliation = authenticationInformation.getOrganizationAffiliation(user.getInstitutionCristinId());
        var updatedUser = existingUser.copy().withAffiliation(affiliation).build();
        identityService.updateUser(updatedUser);
        return updatedUser;
    }

    private UserDto addUser(UserDto user) {
        identityService.addUser(user);
        return user;
    }
}