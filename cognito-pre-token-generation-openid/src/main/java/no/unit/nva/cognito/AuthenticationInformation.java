package no.unit.nva.cognito;

public class AuthenticationInformation /* implements PersonInformation */ {

//    public static final String COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR = "Could not find user for customer: ";
//    private final PersonInformation personInformation;
//    private CustomerDto currentCustomer;
//    private UserDto currentUser;
//
//    public AuthenticationInformation(PersonInformation personInformation) {
//        this.personInformation = personInformation;
//    }
//
//    public CustomerDto getCurrentCustomer() {
//        return currentCustomer;
//    }
//
//    public String extractFirstName() {
//        return getCristinPersonResponse().map(CristinPersonResponse::extractFirstName).orElseThrow();
//    }
//
//    public String extractLastName() {
//        return getCristinPersonResponse().map(CristinPersonResponse::extractLastName).orElseThrow();
//    }
//
//    public Optional<String> getCurrentCustomerId() {
//        return Optional.ofNullable(getCurrentCustomer()).map(CustomerDto::getId).map(URI::toString);
//    }
//
//    public URI getCristinPersonId() {
//        return getCristinPersonResponse().map(CristinPersonResponse::getCristinId).orElseThrow();
//    }
//
//    public CustomerDto updateCurrentCustomer() {
//        this.currentCustomer = returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer();
//        return currentCustomer;
//    }
//
//    public UserDto getCurrentUser() {
//        return currentUser;
//    }
//
//    public void updateCurrentUser(Collection<UserDto> users) {
//        if (nonNull(currentCustomer)) {
//            var currentCustomerId = currentCustomer.getId();
//            this.currentUser = attempt(() -> filterOutUser(users, currentCustomerId))
//                .orElseThrow(fail -> handleUserNotFoundError());
//        }
//    }
//
//    public boolean personExistsInPersonRegistry() {
//        return getCristinPersonResponse().isPresent();
//    }
//
//    @Override
//    public Set<CustomerDto> getActiveCustomers() {
//        return personInformation.getActiveCustomers();
//    }
//
//    @Override
//    public void setActiveCustomers(Set<CustomerDto> activeCustomers) {
//        personInformation.setActiveCustomers(activeCustomers);
//    }
//
//    @Override
//    public String getOrgFeideDomain() {
//        return personInformation.getOrgFeideDomain();
//    }
//
//    @Override
//    public String getPersonFeideIdentifier() {
//        return personInformation.getPersonFeideIdentifier();
//    }
//
//    @Override
//    public Optional<CristinPersonResponse> getCristinPersonResponse() {
//        return personInformation.getCristinPersonResponse();
//    }
//
//    @Override
//    public void setCristinPersonResponse(CristinPersonResponse cristinResponse) {
//        personInformation.setCristinPersonResponse(cristinResponse);
//    }
//
//    @Override
//    public URI getOrganizationAffiliation(URI parentInstitution) {
//        return personInformation.getOrganizationAffiliation(parentInstitution);
//    }
//
//    @Override
//    public List<PersonAffiliation> getPersonAffiliations() {
//        return personInformation.getPersonAffiliations();
//    }
//
//    @Override
//    public void setPersonAffiliations(List<PersonAffiliation> affiliationInformation) {
//        personInformation.setPersonAffiliations(affiliationInformation);
//    }
//
//    @Override
//    public Optional<URI> getPersonRegistryId() {
//        return personInformation.getPersonRegistryId();
//    }
//
//    private IllegalStateException handleUserNotFoundError() {
//        return new IllegalStateException(COULD_NOT_FIND_USER_FOR_CUSTOMER_ERROR + currentCustomer.getId());
//    }
//
//    private UserDto filterOutUser(Collection<UserDto> users, URI currentCustomerId) {
//        return users.stream()
//            .filter(user -> user.getInstitution().equals(currentCustomerId))
//            .collect(SingletonCollector.collect());
//    }
//
//    private CustomerDto returnCurrentCustomerIfDefinedByFeideLoginOrPersonIsAffiliatedToExactlyOneCustomer() {
//        return getActiveCustomers().stream()
//            .filter(this::selectFeideOrgIfApplicable)
//            .collect(SingletonCollector.tryCollect())
//            .orElse(fail -> null);
//    }
//
//    private boolean selectFeideOrgIfApplicable(CustomerDto customer) {
//        return userAuthenticatedWithNin()
//               || getOrgFeideDomain().equals(customer.getFeideOrganizationDomain());
//    }
//
//    private boolean userAuthenticatedWithNin() {
//        return isNull(getOrgFeideDomain());
//    }
}
