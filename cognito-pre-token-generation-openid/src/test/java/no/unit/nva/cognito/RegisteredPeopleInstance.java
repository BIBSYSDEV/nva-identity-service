package no.unit.nva.cognito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.function.Predicate.not;
import static no.unit.nva.cognito.IdentityServiceEntryUpdateHandler.BELONGS_TO;
import static no.unit.nva.cognito.NetworkingUtils.AUTHORIZATION_HEADER;
import static no.unit.nva.cognito.NetworkingUtils.CONTENT_TYPE;
import static no.unit.nva.cognito.cristin.CristinClient.REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE;
import static no.unit.nva.customer.testing.CustomerDataGenerator.randomVocabularies;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cognito.cristin.CristinAffiliation;
import no.unit.nva.cognito.cristin.CristinIdentifier;
import no.unit.nva.cognito.cristin.CristinResponse;
import no.unit.nva.cognito.cristin.NationalIdentityNumber;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.model.CustomerDtoWithoutContext;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;

public class RegisteredPeopleInstance {

    public static final boolean IGNORE_ARRAY_ORDER = true;
    public static final boolean DO_NOT_IGNORE_OTHER_ELEMENTS = false;
    public static final boolean MATCH_CASE = false;
    private final DataportenMock dataporten;
    private final CustomerService customerService;
    private List<NationalIdentityNumber> people;
    private Map<NationalIdentityNumber, CristinResponse> cristinRegistry;
    private IdentityService identityService;

    public RegisteredPeopleInstance(DataportenMock dataporten,
                                    CustomerService customerService,
                                    IdentityService identityService) {
        this.dataporten = dataporten;
        this.customerService = customerService;
        this.identityService = identityService;
    }

    public RegisteredPeopleInstance generateRandomPeople(int numberOfPeople) {
        this.createPeople(randomPeople(numberOfPeople));
        this.createCristinRegistry();
        this.setupCristinResponses();
        this.setupCustomers();
        return this;
    }

    public Stream<URI> fetchNvaCustomersForPersonsInactiveAffiliations(NationalIdentityNumber person) {
        return mapCristinOrgIdsToNvaCustomerIds(getInactiveAffiliationsForPerson(person));
    }

    public Stream<URI> fetchNvaCustomersForPersonsActiveAffiliations(NationalIdentityNumber person) {
        return mapCristinOrgIdsToNvaCustomerIds(getActiveAffiliationsForPerson(person));
    }

    public Stream<URI> mapCristinOrgIdsToNvaCustomerIds(Collection<URI> activeAffiliations) {
        return activeAffiliations.stream()
            .map(cristinOrgId -> customerService.getCustomerByCristinId(cristinOrgId.toString()))
            .map(CustomerDtoWithoutContext::getId);
    }

    public Collection<URI> getCristinOrgUris() {
        return this.cristinRegistry.values().stream()
            .map(CristinResponse::getAffiliations)
            .flatMap(Collection::stream)
            .map(CristinAffiliation::getOrganizationUri)
            .collect(Collectors.toList());
    }

    public List<NationalIdentityNumber> getPeople() {
        return this.people;
    }

    public Collection<URI> getActiveAffiliationsForPerson(NationalIdentityNumber person) {
        return this.cristinRegistry.get(person).getAffiliations()
            .stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .collect(Collectors.toList());
    }

    public Collection<URI> getInactiveAffiliationsForPerson(NationalIdentityNumber person) {
        return this.cristinRegistry.get(person).getAffiliations()
            .stream()
            .filter(not(CristinAffiliation::isActive))
            .map(CristinAffiliation::getOrganizationUri)
            .collect(Collectors.toList());
    }

    public CristinIdentifier getCristinIdentifier(NationalIdentityNumber person) {
        return cristinRegistry.get(person).getIdentifiers().stream().collect(SingletonCollector.collect());
    }


    private static List<NationalIdentityNumber> randomPeople(int numberOfPeople) {
        return IntStream.range(0, numberOfPeople).boxed()
            .map(ignored -> randomString())
            .map(NationalIdentityNumber::new)
            .collect(Collectors.toList());
    }

    private void setupCustomers() {
        this.cristinRegistry.values()
            .stream()
            .map(CristinResponse::getAffiliations)
            .flatMap(Collection::stream)
            .map(CristinAffiliation::getOrganizationUri)
            .forEach(this::createNvaCustomer);
    }

    private void createNvaCustomer(URI cristinOrgId) {
        var customer = CustomerDto.builder()
            .withDisplayName(randomString())
            .withName(randomString())
            .withArchiveName(randomString())
            .withCristinId(cristinOrgId.toString())
            .withVocabularies(randomVocabularies())
            .withCname(randomString())
            .build();
        customerService.createCustomer(customer);
    }

    private void setupCristinResponses() {
        cristinRegistry.keySet().forEach(this::setupCristinServiceResponse);
    }

    private void setupCristinServiceResponse(NationalIdentityNumber nin) {
        stubFor(post("/person/identityNumber")
                    .withHeader(AUTHORIZATION_HEADER,
                                new EqualToPattern("Bearer " + dataporten.getJwtToken(), MATCH_CASE))
                    .withHeader(CONTENT_TYPE, applicationJson())
                    .withRequestBody(cristinServiceRequestBody(nin))
                    .willReturn(aResponse().withStatus(HTTP_OK).withBody(cristinResponseBody(nin))));
    }

    private String cristinResponseBody(NationalIdentityNumber nin) {
        return cristinRegistry.get(nin).toString();
    }

    private StringValuePattern applicationJson() {
        return new EqualToPattern("application/json", MATCH_CASE);
    }

    private ContentPattern<?> cristinServiceRequestBody(NationalIdentityNumber nin) {
        String jsonBody = String.format(REQUEST_TO_CRISTIN_SERVICE_JSON_TEMPLATE, nin.toString());
        return new EqualToJsonPattern(jsonBody, IGNORE_ARRAY_ORDER, DO_NOT_IGNORE_OTHER_ELEMENTS);
    }

    private void createPeople(List<NationalIdentityNumber> people) {
        this.people = people;
    }

    private void createCristinRegistry() {
        this.cristinRegistry = people.stream()
            .map(CristinDataGenerator::createCristinResponse)
            .collect(Collectors.toMap(response -> new NationalIdentityNumber(response.getNin()), response -> response));
    }

    public List<UserDto> createUserEntriesForPerson(NationalIdentityNumber person){
        return createUserEntriesForPerson(cristinRegistry.get(person));
    }
    private List<UserDto> createUserEntriesForPerson(CristinResponse cristinResponse) {
        return cristinResponse.getAffiliations()
            .stream()
            .map(CristinAffiliation::getOrganizationUri)
            .map(cristinOrg -> createUserEntry(cristinOrg, cristinResponse))
            .map(this::insertUserInDatabase)
            .map(user -> identityService.getUser(user))
            .collect(Collectors.toList());


    }

    private UserDto createUserEntry(URI cristinOrg, CristinResponse cristinRecord) {
        var customer = customerService.getCustomerByCristinId(cristinOrg.toString());
        var customerIdentifier = new UriWrapper(customer.getId()).getFilename();
        return UserDto.newBuilder()
            .withFamilyName(cristinRecord.extractLastName())
            .withGivenName(cristinRecord.extractFirstName())
            .withInstitution(customer.getId())
            .withUsername(cristinRecord.getPersonsCristinIdentifier().getValue() + BELONGS_TO + customerIdentifier)
            .withRoles(randomRoles())
            .build();
    }

    private UserDto insertUserInDatabase(UserDto user) {
        user.getRoles().forEach(role -> identityService.addRole(role));
        identityService.addUser(user);
        return user;
    }

    private Collection<RoleDto> randomRoles() {
        return List.of(randomRole(), randomRole());
    }

    private RoleDto randomRole() {
        return RoleDto.newBuilder().withRoleName(randomString()).build();
    }
}
