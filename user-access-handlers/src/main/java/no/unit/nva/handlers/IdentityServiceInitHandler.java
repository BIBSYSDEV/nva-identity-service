package no.unit.nva.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.data.DefaultRoleSource;
import no.unit.nva.handlers.models.RoleList;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.ClientDto;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;

public class IdentityServiceInitHandler extends ApiGatewayHandler<Void, RoleList> {

    public static final URI SIKT_CRISTIN_ID = URI.create(new Environment().readEnv("SIKT_CRISTIN_ID"));
    public static final String SIKT = "Sikt";
    public static final String SIKT_ACTING_USER = "nva-backend@20754.0.0.0";
    public static final String BACKEND_CLIENT_ID_ENV = "BACKEND_CLIENT_ID";
    public static final String SIKT_NO = "sikt.no";
    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceInitHandler.class);
    private final IdentityService identityService;
    private final CustomerService customerService;
    private final RoleSource roleSource;

    @JacocoGenerated
    public IdentityServiceInitHandler() {
        this(IdentityService.defaultIdentityService(), defaultCustomerService(), new DefaultRoleSource(),
             new Environment());
    }

    public IdentityServiceInitHandler(IdentityService identityService, CustomerService customerService,
                                      RoleSource roleSource, Environment environment) {
        super(Void.class, environment);
        this.identityService = identityService;
        this.customerService = customerService;
        this.roleSource = roleSource;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected RoleList processInput(Void input, RequestInfo requestInfo, Context context) {
        var defaultRoles = roleSource.roles()
                               .stream()
                               .map(attempt(this::addOrUpdateRole))
                               .map(attempt -> attempt.toOptional(fail -> logError(fail.getException())))
                               .flatMap(Optional::stream)
                               .collect(Collectors.toSet());

        var sikt = createDefaultCustomer();

        addSiktBackendClientUser(sikt);

        return new RoleList(defaultRoles);
    }

    private void addSiktBackendClientUser(CustomerDto sikt) {
        logger.info("Attempting to add Sikt Backend Client User...");
        // The Cognito user pool client credentials have already been set up in Cognito. However, the client still
        // needs to be added to the database.
        var backendClientId = environment.readEnv(BACKEND_CLIENT_ID_ENV);

        attempt(() -> {
            logger.info("Getting client with ClientId: {}", backendClientId);
            return identityService.getClient(ClientDto.newBuilder().withClientId(backendClientId).build());
        }).or(() -> {
            logger.info("Client not found. Creating new client...");
            var clientDto = ClientDto.newBuilder()
                                .withClientId(backendClientId)
                                .withCustomer(sikt.getId())
                                .withCristinOrgUri(sikt.getCristinId())
                                .withActingUser(SIKT_ACTING_USER)
                                .build();

            identityService.addExternalClient(clientDto);
            return clientDto;
        }).orElseThrow();
    }

    private CustomerDto createDefaultCustomer() {
        logger.info("Attempting to create default customer...");
        var customer = CustomerDto.builder()
                           .withCristinId(SIKT_CRISTIN_ID)
                           .withFeideOrganizationDomain(SIKT_NO)
                           .withCname(SIKT)
                           .withName(SIKT)
                           .withDisplayName(SIKT)
                           .withShortName(SIKT)
                           .withCustomerOf(ApplicationDomain.NVA)
                           .build();

        return attempt(() -> {
            logger.info("Creating customer...");
            return customerService.createCustomer(customer);
        }).or(() -> {
            logger.info("Customer already exists. Fetching customer by org domain: {}", SIKT_NO);
            return customerService.getCustomerByOrgDomain(SIKT_NO);
        }).orElseThrow();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, RoleList output) {
        return HttpURLConnection.HTTP_OK;
    }

    private void logError(Exception exception) {
        logger.warn(exception.getMessage());
    }

    private RoleDto addOrUpdateRole(RoleDto role) throws InvalidInputException, NotFoundException {
        try {
            identityService.addRole(role);
            logger.info("Added role: {}", role);
        } catch (ConflictException e) {
            identityService.updateRole(role);
            logger.info("Updated role: {}", role);
        }
        return role;
    }
}
