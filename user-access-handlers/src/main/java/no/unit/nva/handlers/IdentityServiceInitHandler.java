package no.unit.nva.handlers;

import static no.unit.nva.customer.Constants.defaultCustomerService;
import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.APPROVE_PUBLISH_REQUEST;
import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_PROJECTS;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.apigateway.AccessRight.PROCESS_IMPORT_CANDIDATE;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE_EMBARGO_READ;
import static nva.commons.apigateway.AccessRight.READ_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.REJECT_DOI_REQUEST;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.customer.model.ApplicationDomain;
import no.unit.nva.customer.model.CustomerDto;
import no.unit.nva.customer.service.CustomerService;
import no.unit.nva.database.IdentityService;
import no.unit.nva.handlers.models.RoleList;
import no.unit.nva.useraccessservice.exceptions.InvalidInputException;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ConflictException;
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
    
    @JacocoGenerated
    public IdentityServiceInitHandler() {
        this(IdentityService.defaultIdentityService(), defaultCustomerService());
    }
    
    public IdentityServiceInitHandler(IdentityService identityService, CustomerService customerService) {
        super(Void.class);
        this.identityService = identityService;
        this.customerService = customerService;
    }
    
    @Override
    protected RoleList processInput(Void input, RequestInfo requestInfo, Context context) {
        var defaultRoles = createDefaultRoles()
            .stream()
            .map(attempt(this::addRole))
            .map(attempt -> attempt.toOptional(fail -> logError(fail.getException())))
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
        
        createSiktCustomer();
        
        return new RoleList(defaultRoles);
    }
    
    private void createSiktCustomer() {
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
    
    private RoleDto addRole(RoleDto role) throws ConflictException, InvalidInputException {
        logger.info("Adding role:{}", role);
        identityService.addRole(role);
        return role;
    }
    
    private List<RoleDto> createDefaultRoles() {
        var creator = RoleDto.newBuilder().withRoleName(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT)
                          .build();
        var curator = RoleDto.newBuilder().withRoleName("Curator")
                          .withAccessRights(
                              List.of(APPROVE_DOI_REQUEST,
                                      REJECT_DOI_REQUEST,
                                      READ_DOI_REQUEST,
                                      EDIT_OWN_INSTITUTION_RESOURCES,
                                      APPROVE_PUBLISH_REQUEST))
                          .build();
        var institutionAdmin = RoleDto.newBuilder().withRoleName("Institution-admin")
                                   .withAccessRights(List.of(EDIT_OWN_INSTITUTION_RESOURCES,
                                                             EDIT_OWN_INSTITUTION_PROJECTS,
                                                             EDIT_OWN_INSTITUTION_USERS))
                                   .build();
        var curatorImportCandidate =
            RoleDto.newBuilder()
                .withRoleName("Curator-Import-candidate")
                .withAccessRights(List.of(PROCESS_IMPORT_CANDIDATE))
                .build();
        var curatorThesis =
            RoleDto.newBuilder().withRoleName("Curator-thesis").withAccessRights(List.of(PUBLISH_DEGREE)).build();
        var curatorThesisEmbargo =
            RoleDto.newBuilder()
                .withRoleName("Curator-thesis-embargo")
                .withAccessRights(List.of(PUBLISH_DEGREE_EMBARGO_READ))
                .build();
        var applicationAdmin = RoleDto.newBuilder().withRoleName("App-admin")
                                   .withAccessRights(List.of(ADMINISTRATE_APPLICATION))
                                   .build();

        var editor = RoleDto.newBuilder().withRoleName("Editor")
                         .withAccessRights(List.of(EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW,
                                                   EDIT_ALL_NON_DEGREE_RESOURCES))
                         .build();

        return List.of(creator, curator, institutionAdmin, applicationAdmin, editor, curatorImportCandidate,
                       curatorThesis, curatorThesisEmbargo);
    }
}
