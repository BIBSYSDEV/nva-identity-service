package no.unit.nva.handlers;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.data.DefaultRoleSource;
import no.unit.nva.handlers.models.RoleList;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityServiceInitHandler extends ApiGatewayHandler<Void, RoleList> {
    
    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceInitHandler.class);
    public static final URI SIKT_CRISTIN_ID = URI.create(new Environment().readEnv("SIKT_CRISTIN_ID"));
    public static final String SIKT = "Sikt";
    
    private final IdentityService identityService;
    private final CustomerService customerService;
    private final RoleSource roleSource;
    
    @JacocoGenerated
    public IdentityServiceInitHandler() {
        this(IdentityService.defaultIdentityService(), defaultCustomerService(), new DefaultRoleSource());
    }
    
    public IdentityServiceInitHandler(IdentityService identityService, CustomerService customerService,
                                      RoleSource roleSource) {
        super(Void.class);
        this.identityService = identityService;
        this.customerService = customerService;
        this.roleSource = roleSource;
    }
    
    @Override
    protected RoleList processInput(Void input, RequestInfo requestInfo, Context context) {
        var defaultRoles = roleSource.roles()
            .stream()
            .map(attempt(this::addOrUpdateRole))
            .map(attempt -> attempt.toOptional(fail -> logError(fail.getException())))
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
        
        createDefaultCustomer();
        
        return new RoleList(defaultRoles);
    }
    
    private void createDefaultCustomer() {
        var customer = CustomerDto.builder().withCristinId(SIKT_CRISTIN_ID)
                           .withFeideOrganizationDomain("sikt.no")
                           .withCname(SIKT)
                           .withName(SIKT)
                           .withDisplayName(SIKT)
                           .withShortName(SIKT)
                           .withCustomerOf(ApplicationDomain.NVA)
            .build();
        attempt(() -> customerService.createCustomer(customer)).orElseThrow();
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
