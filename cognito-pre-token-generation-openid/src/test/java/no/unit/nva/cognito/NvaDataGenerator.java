package no.unit.nva.cognito;

import static no.unit.nva.cognito.CognitoClaims.AT;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;

public class NvaDataGenerator {

    private static final boolean ACTIVE = true;
    private static final boolean INACTIVE = !ACTIVE;
    private final RegisteredPeopleInstance registeredPeopleInstance;
    private final CustomerService customerService;

    public NvaDataGenerator(RegisteredPeopleInstance registeredPeopleInstance, CustomerService customerService) {
        this.registeredPeopleInstance = registeredPeopleInstance;
        this.customerService = customerService;
    }

    public static RoleDto createRole() {
        return RoleDto.newBuilder().withRoleName(randomString())
            .withAccessRights(randomAccessRights())
            .build();
    }

    public List<UserDto> createUsers(NationalIdentityNumber nin, boolean includeInactive) {
        HashSet<URI> topLevelAffiliations = calculateTopLevelAffilationsToCreateUsersFor(nin, includeInactive);
        var customers = topLevelAffiliations.stream()
            .map(attempt(customerService::getCustomerByCristinId))
            .map(Try::orElseThrow);
        return customers.map(customer -> createUser(nin, customer)).collect(Collectors.toList());
    }

    public UserDto createUser(NationalIdentityNumber nin, CustomerDto customerDto) {
        String username = createUserName(nin, customerDto);

        return UserDto.newBuilder()
            .withUsername(username)
            .withCristinId(registeredPeopleInstance.getCristinPersonId(nin))
            .withRoles(selectSomeRolesAlreadySavedInIdentityService())
            .withGivenName(randomString())
            .withFamilyName(randomString())
            .withInstitution(customerDto.getId())
            .withInstitutionCristinId(customerDto.getCristinId())
            .build();
    }

    private static Set<AccessRight> randomAccessRights() {
        return new HashSet<>(List.of(randomAccessRight(), randomAccessRight(), randomAccessRight()));
    }

    private static AccessRight randomAccessRight() {
        return randomElement(AccessRight.values());
    }

    private HashSet<URI> calculateTopLevelAffilationsToCreateUsersFor(NationalIdentityNumber nin,
                                                                      boolean includeInactive) {
        var topLevelAffiliations = new HashSet<>(registeredPeopleInstance.getTopLevelAffiliationsForUser(nin, ACTIVE));
        if (includeInactive) {
            topLevelAffiliations.addAll(registeredPeopleInstance.getTopLevelAffiliationsForUser(nin, INACTIVE));
        }
        return topLevelAffiliations;
    }

    private List<RoleDto> selectSomeRolesAlreadySavedInIdentityService() {
        var roles = registeredPeopleInstance.getAvailableNvaRoles().toArray(RoleDto[]::new);
        return List.of(randomElement(roles), randomElement(roles), randomElement(roles));
    }

    private String createUserName(NationalIdentityNumber nin, CustomerDto customerDto) {
        var cristinPersonIdentifier = registeredPeopleInstance.getCristinPersonIdentifier(nin);
        var cristinOrgIdentifier = UriWrapper.fromUri(customerDto.getCristinId()).getLastPathElement();
        return cristinPersonIdentifier.getValue() + AT + cristinOrgIdentifier;
    }
}
