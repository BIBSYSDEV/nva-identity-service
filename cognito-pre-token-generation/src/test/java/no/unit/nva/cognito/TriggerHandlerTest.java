package no.unit.nva.cognito;

import static no.unit.nva.RandomUserDataGenerator.randomCristinOrgId;
import static no.unit.nva.cognito.service.UserApiMock.FIRST_ACCESS_RIGHT;
import static no.unit.nva.cognito.service.UserApiMock.SAMPLE_ACCESS_RIGHTS;
import static no.unit.nva.cognito.service.UserApiMock.SECOND_ACCESS_RIGHT;
import static no.unit.nva.cognito.service.UserPoolEntryUpdater.CUSTOM_APPLICATION_ACCESS_RIGHTS;
import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.useraccessservice.model.ViewingScope.defaultViewingScope;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.UserApiMock;
import no.unit.nva.cognito.service.UserPoolEntryUpdater;
import no.unit.nva.cognito.service.UserService;
import no.unit.nva.useraccessservice.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.UserDto;
import no.unit.nva.useraccessservice.model.ViewingScope;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.SingletonCollector;
import org.javers.common.collections.Lists;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@SuppressWarnings("unchecked")
public class TriggerHandlerTest {

    public static final String SAMPLE_ORG_NUMBER = "1234567890";
    public static final String SAMPLE_HOSTED_ORG_NUMBER = "1234567890";

    public static final String SAMPLE_AFFILIATION = "[member, employee, staff]";
    public static final String SAMPLE_HOSTED_AFFILIATION =
        "[member%40zs.bibsys.no, employee%40zs.bibsys.no, staff%40zs.bibsys.no, error%4zs.bibsys.no]";
    public static final String SAMPLE_HOSTED_FEIDE_ID = "feideId@bibsys.no";

    public static final String EMPTY_AFFILIATION = "[]";
    public static final String SAMPLE_FEIDE_ID = "feideId";
    public static final URI SAMPLE_CUSTOMER_ID = randomUri();

    public static final String SAMPLE_USER_POOL_ID = "userPoolId";
    public static final String SAMPLE_USER_NAME = "userName";
    public static final String SAMPLE_GIVEN_NAME = "givenName";
    public static final String SAMPLE_FAMILY_NAME = "familyName";

    public static final String CREATOR = "Creator";
    public static final String USER = "User";

    public static final AdminUpdateUserAttributesResponse UNUSED_RESULT = null;
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final Context CONTEXT = new FakeContext();
    public static final URI SAMPLE_CRISTIN_ORG_ID = randomCristinOrgId();
    private final AtomicReference<List<AttributeType>> attributeTypesBuffer = new AtomicReference<>();
    private final Context mockContext;
    private CustomerApi customerApi;
    private UserApiMock userApi;
    private TriggerHandler handler;
    private UserPoolEntryUpdater userPoolEntryUpdater;
    private CognitoIdentityProviderClient awsCognitoProvider;
    private UserService userService;

    public TriggerHandlerTest() {
        mockContext = new FakeContext();
    }

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        awsCognitoProvider = mockAwsIdentityProvider();
        setupTriggerHandler();
    }

    @Test
    public void handleRequestUsesExistingUserWhenUserIsFound() throws BadRequestException {
        mockCustomerApiWithExistingCustomer();
        prepareMocksWithExistingUser(createUserWithInstitutionAndCreatorRole());

        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, new FakeContext());

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void shouldSetViewingScopeOnExistingUserWhereViewingScopeIsMissing() throws BadRequestException {
        mockCustomerApiWithExistingCustomer();
        UserDto existingUser = createUserWithInstitutionAndCreatorRole();
        existingUser.setViewingScope(null);
        prepareMocksWithExistingUser(existingUser);

        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, new FakeContext());

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithUserRoleWhenNoCustomerIsFound() throws InvalidEntryInternalException {
        mockCustomerApiWithNoCustomer();

        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, CONTEXT);

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithOnlyUserRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForAffiliatedUser()
        throws InvalidEntryInternalException, BadRequestException {
        mockCustomerApiWithExistingCustomer();

        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, CONTEXT);

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestAddsAccessRightsAttributesToUserPoolAttributesForUserWithRole()
        throws InvalidEntryInternalException, BadRequestException {
        prepareMocksWithExistingUser(createUserWithInstitutionAndCreatorRole());
        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        handler.handleRequest(requestEvent, mockContext);
        verifyNumberOfAttributeUpdatesInCognito(1);

        String accessRight = extractAccessRightsFromUserAttributes();

        assertThat(accessRight, containsString(FIRST_ACCESS_RIGHT));
        assertThat(accessRight, containsString(SECOND_ACCESS_RIGHT));
    }

    @Test
    public void handleRequestAddsAccessRightsAsCsvWhenAccessRightsAreMoreThanOne()
        throws InvalidEntryInternalException, BadRequestException {
        prepareMocksWithExistingUser(createUserWithInstitutionAndCreatorRole());
        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        handler.handleRequest(requestEvent, mockContext);
        verifyNumberOfAttributeUpdatesInCognito(1);

        String accessRightsString = extractAccessRightsFromUserAttributes();
        Set<String> accessRights = toSet(accessRightsString);
        assertThat(accessRights, is((equalTo(SAMPLE_ACCESS_RIGHTS))));
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForNonAffiliatedUser()
        throws InvalidEntryInternalException, BadRequestException {
        mockCustomerApiWithExistingCustomer();

        Map<String, Object> requestEvent = createRequestEventWithEmptyAffiliation();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, new FakeContext());

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndOnlyUserRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(createdUser, expected);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handlerReturnsUserWithoutCreatorRoleWhenUserHadCreatorRoleButNowHasAffiliationNotProvidingTheRole()
        throws InvalidEntryInternalException, BadRequestException {
        prepareMocksWithExistingUser(createUserWithInstitutionAndCreatorRole());
        var currentUser = getUserFromMock();
        List<String> rolesBeforeLogin = extractRoleNames(currentUser);
        assertThat(rolesBeforeLogin, hasItem(CREATOR));
        Map<String, Object> requestEvent = createRequestEventWithEmptyAffiliation();
        handler.handleRequest(requestEvent, mockContext);
        UserDto createdUser = getUserFromMock();
        List<String> rolesAfterLogin = extractRoleNames(createdUser);
        String oldAndNewRolesDiff = compareAssignedRoles(rolesBeforeLogin, rolesAfterLogin);
        assertThat(oldAndNewRolesDiff, rolesAfterLogin, not(hasItem(CREATOR)));
    }

    @Test
    public void handlerReturnsUserWithManuallyAssignedRolesWhenAutomaticallyAssignedRolesAreRemoved()
        throws InvalidEntryInternalException, BadRequestException {
        String manuallyAssignedRole = randomRoleName();
        UserDto existingUser = createUserWithCustomRole(manuallyAssignedRole);
        prepareMocksWithExistingUser(existingUser);

        UserDto currentUser = getUserFromMock();
        final List<String> rolesBeforeLogin = Lists.immutableCopyOf(extractRoleNames(currentUser));

        Map<String, Object> requestEvent = createRequestEventWithEmptyAffiliation();
        handler.handleRequest(requestEvent, mockContext);
        UserDto createdUser = getUserFromMock();

        List<String> rolesAfterLogin = extractRoleNames(createdUser);

        assertThat(rolesBeforeLogin, hasItem(manuallyAssignedRole));
        assertThat(rolesBeforeLogin, hasItem(CREATOR));

        String oldAndNewRolesDiff = compareAssignedRoles(rolesBeforeLogin, rolesAfterLogin);
        assertThat(oldAndNewRolesDiff, rolesAfterLogin, hasItem(manuallyAssignedRole));
        assertThat(oldAndNewRolesDiff, rolesAfterLogin, not(hasItem(CREATOR)));
    }

    @Test
    public void handlerAddsCreatorRoleFromUserThatDidNotHaveCreatorRoleButNowHasAffiliationThatGivesThemTheRole()
        throws InvalidEntryInternalException, BadRequestException {
        prepareMocksWithExistingUser(createUserWithInstitutionAndOnlyUserRole());
        UserDto currentUser = getUserFromMock();
        List<String> rolesBeforeLogin = extractRoleNames(currentUser);
        assertThat(rolesBeforeLogin, not(hasItem(CREATOR)));
        Map<String, Object> requestEvent = createRequestEventWithInstitutionAndEduPersonAffiliation();
        handler.handleRequest(requestEvent, mockContext);
        UserDto createdUser = getUserFromMock();

        List<String> rolesAfterLogin = extractRoleNames(createdUser);

        String oldAndNewRolesDiff = compareAssignedRoles(rolesBeforeLogin, rolesAfterLogin);
        assertThat(oldAndNewRolesDiff, rolesAfterLogin, hasItem(CREATOR));
    }

    @Test
    public void handleRequestReturnsNewUserWithUserRoleWhenUserIsFeideHostedUser()
        throws InvalidEntryInternalException {
        mockCustomerApiWithNoCustomer();

        Map<String, Object> requestEvent = createRequestEventWithCompleteBibsysHostedUser();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, new FakeContext());

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = hostedUserWithUserRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestReturnsNewUserWithCreatorRoleWhenUserIsFeideHostedUser()
        throws InvalidEntryInternalException, BadRequestException {
        mockCustomerApiWithNoCustomer();
        mockCustomerApiWithExistingCustomer();

        Map<String, Object> requestEvent = createRequestEventWithCompleteBibsysHostedUser();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, new FakeContext());

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = hostedUserWithCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestReturnsUserWithUserRoleWhenUserIsFeideHostedUserAndUserMissingAffiliation()
        throws InvalidEntryInternalException {
        mockCustomerApiWithNoCustomer();
        Map<String, Object> requestEvent = createRequestEventWithIncompleteBibsysHostedUser();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, new FakeContext());

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = hostedUserWithUserRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    private void setupTriggerHandler() {
        attributeTypesBuffer.set(null);
        customerApi = mock(CustomerApi.class);
        userApi = new UserApiMock();
        userPoolEntryUpdater = new UserPoolEntryUpdater(awsCognitoProvider);
        userService = new UserService(userApi);
        handler = new TriggerHandler(userService, customerApi, userPoolEntryUpdater);
    }

    private String randomRoleName() {
        return UUID.randomUUID().toString();
    }

    private String compareAssignedRoles(List<String> rolesBeforeLogin, List<String> rolesAfterLogin) {
        return JAVERS.compare(rolesBeforeLogin, rolesAfterLogin).prettyPrint();
    }

    private UserDto createUserWithCustomRole(String manuallyAssignedRole)
        throws InvalidEntryInternalException, BadRequestException {

        UserDto sampleUser = createUserWithInstitutionAndCreatorRole();
        Set<RoleDto> roles = sampleUser.getRoles();

        roles.add(createRole(manuallyAssignedRole));
        return userWithInstitution(userWithRoles(roles));
    }

    private List<String> extractRoleNames(UserDto currentUser) {
        return currentUser.getRoles()
            .stream()
            .map(RoleDto::getRoleName)
            .collect(Collectors.toList());
    }

    private Set<String> toSet(String csv) {
        String[] values = csv.split(TriggerHandler.COMMA_DELIMITER);
        return Arrays.stream(values).collect(Collectors.toSet());
    }

    private String extractAccessRightsFromUserAttributes() {
        return attributeTypesBuffer.get()
            .stream()
            .filter(attr -> attr.name().equals(CUSTOM_APPLICATION_ACCESS_RIGHTS))
            .map(AttributeType::value)
            .collect(SingletonCollector.collect());
    }

    private CognitoIdentityProviderClient mockAwsIdentityProvider() {
        CognitoIdentityProviderClient provider = mock(CognitoIdentityProviderClient.class);
        when(provider.adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class)))
            .thenAnswer(this::storeUserAttributes);
        when(provider.adminGetUser(any(AdminGetUserRequest.class)))
            .thenAnswer(this::returnUserAttributes);
        return provider;
    }

    private AdminGetUserResponse returnUserAttributes(InvocationOnMock invocationOnMock) {
        return AdminGetUserResponse.builder().userAttributes(attributeTypesBuffer.get()).build();
    }

    private AdminUpdateUserAttributesResponse storeUserAttributes(InvocationOnMock invocation) {
        AdminUpdateUserAttributesRequest request = invocation.getArgument(0);
        attributeTypesBuffer.set(request.userAttributes());
        return UNUSED_RESULT;
    }

    private void verifyNumberOfAttributeUpdatesInCognito(int numberOfUpdates) {
        verify(awsCognitoProvider, times(numberOfUpdates))
            .adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));
    }

    private UserDto getUserFromMock() {
        return userApi.getUser(SAMPLE_FEIDE_ID).get();
    }

    private UserDto prepareMocksWithExistingUser(UserDto userDto) {
        userApi.createUser(userDto);
        return userDto;
    }

    private void mockCustomerApiWithExistingCustomer() {
        when(customerApi.getCustomer(anyString()))
            .thenReturn(Optional.of(new CustomerResponse(SAMPLE_CUSTOMER_ID,SAMPLE_CRISTIN_ORG_ID.toString())));
    }

    private void mockCustomerApiWithNoCustomer() {
        when(customerApi.getCustomer(anyString())).thenReturn(Optional.empty());
    }

    private UserDto createUserWithOnlyUserRole() throws InvalidEntryInternalException {
        return userWithRoles(Set.of(createRole(USER)));
    }

    private UserDto userWithRoles(Set<RoleDto> roles) throws InvalidEntryInternalException {
        return UserDto.newBuilder()
            .withUsername(SAMPLE_FEIDE_ID)
            .withGivenName(SAMPLE_GIVEN_NAME)
            .withFamilyName(SAMPLE_FAMILY_NAME)
            .withRoles(roles)
            .build();
    }

    private UserDto createUserWithInstitutionAndCreatorRole() throws InvalidEntryInternalException,
                                                                     BadRequestException {
        HashSet<RoleDto> roles = new HashSet<>();
        roles.add(createRole(CREATOR));
        roles.add(createRole(USER));
        return userWithInstitution(userWithRoles(roles));
    }

    private RoleDto createRole(String roleName) throws InvalidEntryInternalException {
        return RoleDto.newBuilder()
            .withName(roleName)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();
    }

    private UserDto createUserWithInstitutionAndOnlyUserRole() throws InvalidEntryInternalException,
                                                                      BadRequestException {
        Set<RoleDto> roles = new HashSet<>();
        roles.add(createRole(USER));
        return userWithInstitution(userWithRoles(roles));
    }

    private UserDto userWithInstitution(UserDto user) throws InvalidEntryInternalException {
        ViewingScope viewingScope = ViewingScope.defaultViewingScope(SAMPLE_CRISTIN_ORG_ID);
        return user.copy().withInstitution(SAMPLE_CUSTOMER_ID).withViewingScope(viewingScope).build();
    }

    private Map<String, Object> createRequestEventWithInstitutionAndEduPersonAffiliation() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(SAMPLE_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return defaultRestObjectMapper.convertValue(event, Map.class);
    }

    private Map<String, Object> createRequestEventWithEmptyAffiliation() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(EMPTY_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return defaultRestObjectMapper.convertValue(event, Map.class);
    }

    private Map<String, Object> createRequestEventWithCompleteBibsysHostedUser() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_HOSTED_FEIDE_ID);
        userAttributes.setHostedOrgNumber(SAMPLE_HOSTED_ORG_NUMBER);
        userAttributes.setHostedAffiliation(SAMPLE_HOSTED_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return defaultRestObjectMapper.convertValue(event, Map.class);
    }

    private UserDto hostedUserWithUserRole() throws InvalidEntryInternalException {
        List<RoleDto> roles = new ArrayList<>();
        roles.add(createRole(USER));
        return UserDto.newBuilder()
            .withUsername(SAMPLE_HOSTED_FEIDE_ID)
            .withGivenName(SAMPLE_GIVEN_NAME)
            .withFamilyName(SAMPLE_FAMILY_NAME)
            .withRoles(roles)
            .build();
    }

    private UserDto hostedUserWithCreatorRole() throws InvalidEntryInternalException, BadRequestException {
        List<RoleDto> roles = new ArrayList<>();
        roles.add(createRole(CREATOR));
        roles.add(createRole(USER));
        return UserDto.newBuilder()
            .withUsername(SAMPLE_HOSTED_FEIDE_ID)
            .withGivenName(SAMPLE_GIVEN_NAME)
            .withFamilyName(SAMPLE_FAMILY_NAME)
            .withRoles(roles)
            .withInstitution(SAMPLE_CUSTOMER_ID)
            .withViewingScope(defaultViewingScope(SAMPLE_CRISTIN_ORG_ID))
            .build();
    }

    private Map<String, Object> createRequestEventWithIncompleteBibsysHostedUser() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_HOSTED_FEIDE_ID);
        userAttributes.setHostedOrgNumber(SAMPLE_HOSTED_ORG_NUMBER);
        userAttributes.setHostedAffiliation(EMPTY_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return defaultRestObjectMapper.convertValue(event, Map.class);
    }
}
