package no.unit.nva.cognito;

import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.BELONGS_TO;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomElement;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomString;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import no.unit.useraccessserivce.accessrights.AccessRight;
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
        var topLevelAffiliations = new HashSet<>(registeredPeopleInstance.getTopLevelAffiliationsForUser(nin, ACTIVE));
        if (includeInactive) {
            topLevelAffiliations.addAll(registeredPeopleInstance.getTopLevelAffiliationsForUser(nin, INACTIVE));
        }
        var customers = topLevelAffiliations.stream()
            .map(affiliation -> customerService.getCustomerByCristinId(affiliation.toString()));
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
            .build();
    }

    private List<RoleDto> selectSomeRolesAlreadySavedInIdentityService() {
        var roles = registeredPeopleInstance.getAvailableNvaRoles().toArray(RoleDto[]::new);
        return List.of(randomElement(roles),randomElement(roles),randomElement(roles));
    }

    private static Set<String> randomAccessRights() {
        return new HashSet<>(List.of(randomAccessRight(), randomAccessRight(), randomAccessRight()));
    }

    private static String randomAccessRight() {
        return randomElement(AccessRight.values()).toString();
    }

    private String createUserName(NationalIdentityNumber nin, CustomerDto customerDto) {
        var cristinPersonIdentifier = registeredPeopleInstance.getCristinPersonIdentifier(nin);
        var cristinOrgIdentifier = UriWrapper.fromUri(customerDto.getCristinId()).getLastPathElement();
        return cristinPersonIdentifier.getValue() + BELONGS_TO + cristinOrgIdentifier;
    }
}
