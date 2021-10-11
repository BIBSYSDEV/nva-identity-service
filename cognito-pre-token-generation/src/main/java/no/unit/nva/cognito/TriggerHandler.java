package no.unit.nva.cognito;

import static java.util.Objects.nonNull;
import static no.unit.nva.cognito.util.OrgNumberCleaner.removeCountryPrefix;
import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.customer.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.CustomerDbClient;
import no.unit.nva.cognito.service.UserDbClient;
import no.unit.nva.cognito.service.UserDetails;
import no.unit.nva.cognito.service.UserPoolEntryUpdater;
import no.unit.nva.cognito.service.UserService;
import no.unit.nva.customer.RestConfig;
import no.unit.nva.database.DatabaseServiceImpl;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    public static final String COMMA_DELIMITER = ",";

    public static final String BIBSYS_HOST = "@bibsys.no";
    public static final String EMPTY_STRING = "";
    public static final int START_OF_STRING = 0;
    public static final String TRAILING_BRACKET = "]";
    public static final char AFFILIATION_PART_SEPARATOR = '@';
    public static final String COMMA_SPACE = ", ";
    public static final String COMMA = ",";
    public static final String HOSTED_AFFILIATION_MESSAGE =
        "Overriding orgNumber({}) with hostedOrgNumber({}) and hostedAffiliation";
    public static final String PROBLEM_DECODING_HOSTED_USERS_AFFILIATION =
        "Problem decoding hosted users affiliation, using original";
    private static final Logger logger = LoggerFactory.getLogger(TriggerHandler.class);
    private final UserService userService;
    private final CustomerApi customerApi;
    private final UserPoolEntryUpdater userPoolEntryUpdater;

    @JacocoGenerated
    public TriggerHandler() {
        this(defaultUserService(), defaultCustomerDbClient(), new UserPoolEntryUpdater());
    }

    public TriggerHandler(UserService userService, CustomerApi customerApi,
                          UserPoolEntryUpdater userPoolEntryUpdater) {
        this.userService = userService;
        this.customerApi = customerApi;
        this.userPoolEntryUpdater = userPoolEntryUpdater;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        long start = System.currentTimeMillis();
        Event event = parseEventFromInput(input);


        UserDetails userDetails = extractUserDetails(event);
        UserDto user = getAndUpdateUserDetails(userDetails);

        updateUserDetailsInUserPool(userDetails, user);

        logger.info("handleRequest took {} ms", System.currentTimeMillis() - start);
        return input;
    }

    @JacocoGenerated
    private static CustomerDbClient defaultCustomerDbClient() {
        return new CustomerDbClient(defaultCustomerService());
    }


    @JacocoGenerated
    private static UserService defaultUserService() {
        return new UserService(defaultUserDbClient());
    }

    @JacocoGenerated
    private static UserDbClient defaultUserDbClient() {
        return new UserDbClient(new DatabaseServiceImpl());
    }

    private UserDetails extractUserDetails(Event event) {
        UserAttributes userAttributes = event.getRequest().getUserAttributes();
        if (userIsBibsysHosted(userAttributes)) {
            injectInformationForBibsysHostedCustomer(userAttributes);
        }
        return createUserDetails(event);
    }

    private void injectInformationForBibsysHostedCustomer(UserAttributes userAttributes) {
        logger.info(HOSTED_AFFILIATION_MESSAGE, userAttributes.getOrgNumber(), userAttributes.getHostedOrgNumber());
        userAttributes.setOrgNumber(userAttributes.getHostedOrgNumber());
        userAttributes.setAffiliation(extractAffiliationFromHostedUSer(userAttributes.getHostedAffiliation()));
    }

    private UserDetails createUserDetails(Event event) {
        return Optional.of(event)
            .map(Event::getRequest)
            .map(Request::getUserAttributes)
            .map(UserAttributes::getOrgNumber)
            .flatMap(orgNum -> mapOrgNumberToCustomer(removeCountryPrefix(orgNum)))
            .map(customer -> new UserDetails(event, customer))
            .orElse(new UserDetails(event));
    }

    /**
     * Using ObjectMapper to convert input to Event because we are interested in only some input properties but have no
     * way of telling Lambda's JSON parser to ignore the rest.
     *
     * @param input event json as map
     * @return event
     */
    private Event parseEventFromInput(Map<String, Object> input) {
        return defaultRestObjectMapper.convertValue(input, Event.class);
    }

    private void updateUserDetailsInUserPool(UserDetails userDetails,UserDto user) {
        long start = System.currentTimeMillis();
        userPoolEntryUpdater.updateUserAttributes(userDetails,user);
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
