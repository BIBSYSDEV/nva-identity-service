package no.unit.nva.cognito;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.CustomerDbClient;
import no.unit.nva.cognito.service.UserDbClient;
import no.unit.nva.cognito.service.UserDetails;
import no.unit.nva.cognito.service.UserService;
import no.unit.nva.customer.model.CustomerMapper;
import no.unit.nva.customer.service.impl.DynamoDBCustomerService;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.Constants.DYNAMODB_CLIENT;
import static no.unit.nva.cognito.Constants.ENVIRONMENT;
import static no.unit.nva.cognito.Constants.ID_NAMESPACE_VALUE;
import static no.unit.nva.cognito.util.OrgNumberCleaner.removeCountryPrefix;
import static no.unit.nva.customer.ObjectMapperConfig.objectMapper;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;

public class TriggerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    public static final String CUSTOM_APPLICATION_ROLES = "custom:applicationRoles";
    public static final String CUSTOM_APPLICATION = "custom:application";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String CUSTOM_IDENTIFIERS = "custom:identifiers";
    public static final String CUSTOM_CRISTIN_ID = "custom:cristinId";
    public static final String CUSTOM_APPLICATION_ACCESS_RIGHTS = "custom:accessRights";

    public static final String COMMA_DELIMITER = ",";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";
    public static final String BIBSYS_HOST = "@bibsys.no";
    public static final String EMPTY_STRING = "";
    public static final int START_OF_STRING = 0;
    public static final String TRAILING_BRACKET = "]";
    public static final char AFFILIATION_PART_SEPARATOR = '@';
    public static final String COMMA_SPACE = ", ";
    public static final String COMMA = ",";
    public static final String APPLICATION_ROLES_MESSAGE = "applicationRoles: ";
    public static final String HOSTED_AFFILIATION_MESSAGE =
            "Overriding orgNumber({}) with hostedOrgNumber({}) and hostedAffiliation";
    public static final String PROBLEM_DECODING_HOSTED_USERS_AFFILIATION =
            "Problem decoding hosted users affiliation, using original";
    private static final Logger logger = LoggerFactory.getLogger(TriggerHandler.class);
    private final UserService userService;
    private final CustomerApi customerApi;

    @JacocoGenerated
    public TriggerHandler() {
        this(defaultUserService(), defaultCustomerDbClient());
    }

    public TriggerHandler(UserService userService, CustomerApi customerApi) {
        this.userService = userService;
        this.customerApi = customerApi;
    }

    @JacocoGenerated
    private static CustomerDbClient defaultCustomerDbClient() {
        return new CustomerDbClient(defaultCustomerService(), defaultCustomerMapper());
    }

    @JacocoGenerated
    private static DynamoDBCustomerService defaultCustomerService() {
        return new DynamoDBCustomerService(
                DYNAMODB_CLIENT,
                objectMapper,
                ENVIRONMENT);
    }

    @JacocoGenerated
    private static CustomerMapper defaultCustomerMapper() {
        return new CustomerMapper(ID_NAMESPACE_VALUE);
    }

    @JacocoGenerated
    private static UserService defaultUserService() {
        return new UserService(
                defaultUserDbClient(),
                AWSCognitoIdentityProviderClient.builder().build()
        );
    }

    @JacocoGenerated
    private static UserDbClient defaultUserDbClient() {
        return new UserDbClient(new DatabaseServiceImpl(DYNAMODB_CLIENT, ENVIRONMENT));
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        long start = System.currentTimeMillis();
        Event event = parseEventFromInput(input);

        String userPoolId = event.getUserPoolId();
        String userName = event.getUserName();
        UserDetails userDetails = extractUserDetails(event);
        UserDto user = getAndUpdateUserDetails(userDetails);

        updateUserDetailsInUserPool(userPoolId, userName, userDetails, user);

        logger.info("handleRequest took {} ms", System.currentTimeMillis() - start);
        return input;
    }

    private UserDetails extractUserDetails(Event event) {
        UserAttributes userAttributes = event.getRequest().getUserAttributes();
        if (userIsBibsysHosted(userAttributes)) {
            injectInformationForBibsysHostedCustomer(userAttributes);
        }
        return createUserDetails(userAttributes);
    }

    private void injectInformationForBibsysHostedCustomer(UserAttributes userAttributes) {
        logger.info(HOSTED_AFFILIATION_MESSAGE, userAttributes.getOrgNumber(), userAttributes.getHostedOrgNumber());
        userAttributes.setOrgNumber(userAttributes.getHostedOrgNumber());
        userAttributes.setAffiliation(extractAffiliationFromHostedUSer(userAttributes.getHostedAffiliation()));
    }

    private UserDetails createUserDetails(UserAttributes userAttributes) {
        return Optional.ofNullable(userAttributes.getOrgNumber())
                .flatMap(orgNum -> mapOrgNumberToCustomer(removeCountryPrefix(orgNum)))
                .map(customer -> new UserDetails(userAttributes, customer))
                .orElse(new UserDetails(userAttributes));
    }

    /**
     * Using ObjectMapper to convert input to Event because we are interested in only some input properties but have no
     * way of telling Lambda's JSON parser to ignore the rest.
     *
     * @param input event json as map
     * @return event
     */
    private Event parseEventFromInput(Map<String, Object> input) {
        return JsonUtils.objectMapper.convertValue(input, Event.class);
    }

    private void updateUserDetailsInUserPool(String userPoolId,
                                             String userName,
                                             UserDetails userDetails,
                                             UserDto user) {
        long start = System.currentTimeMillis();
        List<AttributeType> cognitoUserAttributes = createUserAttributes(userDetails, user);
        userService.updateUserAttributes(userPoolId, userName, cognitoUserAttributes);
        logger.info("updateUserDetailsInUserPool took {} ms", System.currentTimeMillis() - start);
    }

    private UserDto getAndUpdateUserDetails(UserDetails userDetails) {
        return userService.getUser(userDetails.getFeideId())
                .map(attempt(user -> userService.updateUser(user, userDetails)))
                .map(Try::orElseThrow)
                .orElseGet(() -> userService.createUser(userDetails));
    }

    private Optional<CustomerResponse> mapOrgNumberToCustomer(String orgNumber) {
        return customerApi.getCustomer(orgNumber);
    }

    private List<AttributeType> createUserAttributes(UserDetails userDetails, UserDto user) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        if (user.getInstitution() != null) {
            userAttributeTypes.add(toAttributeType(CUSTOM_CUSTOMER_ID, user.getInstitution()));
        }
        userDetails.getCristinId()
                .ifPresent(cristinId -> userAttributeTypes.add(toAttributeType(CUSTOM_CRISTIN_ID, cristinId)));

        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION, NVA));
        userAttributeTypes.add(toAttributeType(CUSTOM_IDENTIFIERS, FEIDE_PREFIX + userDetails.getFeideId()));

        String applicationRoles = applicationRolesString(user);
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION_ROLES, applicationRoles));

        String accessRightsString = accessRightsString(user);
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION_ACCESS_RIGHTS, accessRightsString));

        return userAttributeTypes;
    }

    private String accessRightsString(UserDto user) {
        if (!user.getAccessRights().isEmpty()) {
            return toCsv(user.getAccessRights(), s -> s);
        } else {
            return EMPTY_STRING;
        }
    }

    private String applicationRolesString(UserDto user) {
        String applicationRoles = toCsv(user.getRoles(), RoleDto::getRoleName);
        logger.info(APPLICATION_ROLES_MESSAGE + applicationRoles);
        return applicationRoles;
    }

    private AttributeType toAttributeType(String name, String value) {
        AttributeType attributeType = new AttributeType();
        attributeType.setName(name);
        attributeType.setValue(value);
        return attributeType;
    }

    private <T> String toCsv(Collection<T> roles, Function<T, String> stringRepresentation) {
        return roles
                .stream()
                .map(stringRepresentation)
                .collect(Collectors.joining(COMMA_DELIMITER));
    }

    private boolean userIsBibsysHosted(UserAttributes userAttributes) {
        return userAttributes.getFeideId().endsWith(BIBSYS_HOST)
                && nonNull(userAttributes.getHostedOrgNumber());
    }

    private String extractAffiliationFromHostedUSer(String hostedAffiliation) {

        List<String> shortenedAffiliations = Arrays.stream(hostedAffiliation.split(COMMA))
                .map(this::decodeAffiliation)
                .map(this::extractAffiliation)
                .map(String::strip)
                .collect(Collectors.toList());

        return String.join(COMMA_SPACE, shortenedAffiliations).concat(TRAILING_BRACKET);
    }

    private String extractAffiliation(String hostedAffiliation) {
        if (isNotBlank(hostedAffiliation) && hostedAffiliation.contains(String.valueOf(AFFILIATION_PART_SEPARATOR))) {
            return hostedAffiliation.substring(START_OF_STRING, hostedAffiliation.indexOf(AFFILIATION_PART_SEPARATOR));
        } else {
            return EMPTY_STRING;
        }
    }

    private String decodeAffiliation(String hostedAffiliation) {
        return attempt(() -> URLDecoder.decode(hostedAffiliation, StandardCharsets.UTF_8))
                .orElse(fail -> logWarningAndReturnOriginalValue(fail, hostedAffiliation));
    }

    private String logWarningAndReturnOriginalValue(Failure<String> fail, String hostedAffiliation) {
        logger.warn(PROBLEM_DECODING_HOSTED_USERS_AFFILIATION, fail.getException());
        return hostedAffiliation;
    }
}
