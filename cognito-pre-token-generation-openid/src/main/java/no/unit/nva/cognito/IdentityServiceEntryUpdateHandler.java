package no.unit.nva.cognito;

import static no.unit.nva.cognito.EnvironmentVariables.AWS_REGION;
import static no.unit.nva.cognito.EnvironmentVariables.COGNITO_HOST;
import static no.unit.nva.cognito.NetworkingUtils.CRISTIN_HOST;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.IdentityService.defaultIdentityService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.ClaimsOverrideDetails;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.GroupConfiguration;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEvent.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinClient;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class IdentityServiceEntryUpdateHandler
    implements RequestHandler<CognitoUserPoolPreTokenGenerationEvent, CognitoUserPoolPreTokenGenerationEvent> {

    public static final String NIN_FOR_FEIDE_USERS = "custom:feideidnin";
    public static final String NIN_FON_NON_FEIDE_USERS = "custom:nin";
    public static final String FEIDE_ID = "custom:feideid";
    public static final String BELONGS_TO = "@";

    private final RequestAuthorizer requestAuthorizer;
    private final CristinClient cristinClient;
    private final CustomerService customerService;
    private final IdentityService identityService;

    @JacocoGenerated
    public IdentityServiceEntryUpdateHandler() {
        this(defaultCognitoClient(), HttpClient.newHttpClient(), defaultCognitoUri(), CRISTIN_HOST,
             defaultCustomerService(), defaultIdentityService());
    }

    public IdentityServiceEntryUpdateHandler(CognitoIdentityProviderClient cognitoClient,
                                             HttpClient httpClient,
                                             URI cognitoHost,
                                             URI cristinHost,
                                             CustomerService customerService,
                                             IdentityService identityService) {

        this.requestAuthorizer = new RequestAuthorizer(cognitoClient, cognitoHost, httpClient);
        this.cristinClient = new CristinClient(cristinHost, httpClient);
        this.customerService = customerService;
        this.identityService = identityService;
    }

    @Override
    public CognitoUserPoolPreTokenGenerationEvent handleRequest(CognitoUserPoolPreTokenGenerationEvent input,
                                                                Context context) {

        var userAttributes = input.getRequest().getUserAttributes();
        var nin = extractNin(userAttributes);

        CristinPersonResponse cristinResponse = fetchPersonInformationFromCristin(input, nin);
        var activeCustomers = fetchCustomersForActiveAffiliations(cristinResponse);

        var personsUsers = createOrFetchUserEntriesForPerson(cristinResponse, activeCustomers);
        var accessRights = accessRightsPerCustomer(personsUsers);
        injectAccessRightsToEventResponse(input, accessRights);
        return input;
    }

    @JacocoGenerated
    private static URI defaultCognitoUri() {
        try {
            return new URI("https", COGNITO_HOST, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @JacocoGenerated
    private static CognitoIdentityProviderClient defaultCognitoClient() {
        return CognitoIdentityProviderClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .region(AWS_REGION)
            .build();
    }

    private void injectAccessRightsToEventResponse(CognitoUserPoolPreTokenGenerationEvent input,
                                                   String... groupsToOverride) {
        input.setResponse(
            Response.builder().withClaimsOverrideDetails(buildGroupsToOverride(groupsToOverride)).build());
    }

    private String[] accessRightsPerCustomer(List<UserDto> personsUsers) {
        return personsUsers.stream()
            .map(user -> CustomerAccessRight.fromUser(user, customerService))
            .flatMap(Collection::stream)
            .map(CustomerAccessRight::asStrings)
            .flatMap(Collection::stream)
            .toArray(String[]::new);
    }

    private ClaimsOverrideDetails buildGroupsToOverride(String... groupsToOverride) {
        return ClaimsOverrideDetails.builder()
            .withGroupOverrideDetails(GroupConfiguration.builder().withGroupsToOverride(groupsToOverride).build())
            .build();
    }

    private List<UserDto> createOrFetchUserEntriesForPerson(CristinPersonResponse cristinResponse,
                                                            Set<CustomerDto> activeCustomers) {

        return activeCustomers.stream()
            .map(customer -> createNewUserObject(customer, cristinResponse))
            .map(this::getExistingUserOrCreateNew)
            .collect(Collectors.toList());
    }

    private UserDto getExistingUserOrCreateNew(UserDto user) {
        return attempt(() -> identityService.getUser(user))
            .or(() -> addUser(user))
            .orElseThrow();
    }

    private UserDto addUser(UserDto user) {
        identityService.addUser(user);
        return user;
    }

    private UserDto createNewUserObject(CustomerDto customer, CristinPersonResponse cristinResponse) {
        var cristinIdentifier = cristinResponse.getPersonsCristinIdentifier().getValue();

        return UserDto.newBuilder()
            .withUsername(formatUsername(customer, cristinIdentifier))
            .withInstitution(customer.getId())
            .withGivenName(cristinResponse.extractFirstName())
            .withFamilyName(cristinResponse.extractLastName())
            .withCristinId(cristinResponse.getCristinId())
            .build();
    }

    private String formatUsername(CustomerDto customer, String cristinIdentifier) {
        return cristinIdentifier + BELONGS_TO + UriWrapper.fromUri(customer.getCristinId()).getLastPathElement();
    }

    private Set<CustomerDto> fetchCustomersForActiveAffiliations(CristinPersonResponse cristinResponse) {

        return cristinResponse.getAffiliations().stream()
            .filter(CristinAffiliation::isActive)
            .map(CristinAffiliation::getOrganizationUri)
            .map(this::fetchTopLevelOrgUri)
            .map(uri -> customerService.getCustomerByCristinId(uri.toString()))
            .collect(Collectors.toSet());
    }

    private Object fetchTopLevelOrgUri(URI orgUri) {
        return attempt(() -> cristinClient.fetchTopLevelOrgUri(orgUri)).orElseThrow();
    }

    private CristinPersonResponse fetchPersonInformationFromCristin(CognitoUserPoolPreTokenGenerationEvent input,
                                                                    String nin) {
        var jwtToken = requestAuthorizer.fetchJwtToken(input.getUserPoolId());
        return attempt(() -> cristinClient.sendRequestToCristin(jwtToken, nin)).orElseThrow();
    }

    private String extractNin(Map<String, String> userAttributes) {
        return Optional.ofNullable(userAttributes.get(NIN_FOR_FEIDE_USERS))
            .or(() -> Optional.ofNullable(userAttributes.get(NIN_FON_NON_FEIDE_USERS)))
            .orElseThrow();
    }
}
