package no.unit.nva.handlers.data;

import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static nva.commons.apigateway.AccessRight.ACT_AS;
import static nva.commons.apigateway.AccessRight.MANAGE_CUSTOMERS;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_EXTERNAL_CLIENTS;
import static nva.commons.apigateway.AccessRight.MANAGE_IMPORT;
import static nva.commons.apigateway.AccessRight.MANAGE_NVI;
import static nva.commons.apigateway.AccessRight.MANAGE_NVI_CANDIDATES;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_AFFILIATION;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_RESOURCES;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import java.util.List;
import no.unit.nva.handlers.RoleSource;
import no.unit.nva.useraccessservice.model.RoleDto;

public class DefaultRoleSource implements RoleSource {

    protected static final String PUBLISHING_CURATOR_ROLE_NAME = "Publishing-Curator";
    protected static final String DOI_CURATOR_ROLE_NAME = "Doi-Curator";
    protected static final String NVI_CURATOR_ROLE_NAME = "Nvi-Curator";
    protected static final String SUPPORT_CURATOR_ROLE_NAME = "Support-Curator";
    protected static final String INSTITUTION_ADMIN_ROLE_NAME = "Institution-admin";
    protected static final String INTERNAL_IMPORTER_ROLE_NAME = "Internal-importer";
    protected static final String CURATOR_THESIS_ROLE_NAME = "Curator-thesis";
    protected static final String CURATOR_THESIS_EMBARGO_ROLE_NAME = "Curator-thesis-embargo";
    public static final String APP_ADMIN_ROLE_NAME = "App-admin";
    protected static final String EDITOR_ROLE_NAME = "Editor";

    private static final RoleDto CREATOR_ROLE = RoleDto.newBuilder()
                                                    .withRoleName(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT)
                                                    .withAccessRights(List.of(MANAGE_OWN_RESOURCES))
                                                    .build();

    public static final RoleDto PUBLISHING_CURATOR_ROLE = RoleDto.newBuilder()
                                                    .withRoleName(PUBLISHING_CURATOR_ROLE_NAME)
                                                    .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                                                                              MANAGE_PUBLISHING_REQUESTS))
                                                    .build();
    public static final RoleDto NVI_CURATOR_ROLE = RoleDto.newBuilder()
                                                   .withRoleName(NVI_CURATOR_ROLE_NAME)
                                                   .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                                                                             MANAGE_NVI_CANDIDATES))
                                                   .build();
    public static final RoleDto DOI_CURATOR_ROLE = RoleDto.newBuilder()
                                                   .withRoleName(DOI_CURATOR_ROLE_NAME)
                                                   .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                                                                             MANAGE_DOI))
                                                   .build();
    public static final RoleDto SUPPORT_CURATOR_ROLE = RoleDto.newBuilder()
                                                   .withRoleName(SUPPORT_CURATOR_ROLE_NAME)
                                                   .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                                                                             SUPPORT))
                                                   .build();
    private static final RoleDto INSTITUTION_ADMIN_ROLE = RoleDto.newBuilder()
                                                              .withRoleName(INSTITUTION_ADMIN_ROLE_NAME)
                                                              .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                                                                                        MANAGE_OWN_AFFILIATION))
                                                              .build();

    private static final RoleDto INTERNAL_IMPORTER = RoleDto.newBuilder()
                                                                     .withRoleName(INTERNAL_IMPORTER_ROLE_NAME)
                                                                     .withAccessRights(
                                                                         List.of(MANAGE_IMPORT))
                                                                     .build();

    private static final RoleDto CURATOR_THESIS_ROLE = RoleDto.newBuilder()
                                                           .withRoleName(CURATOR_THESIS_ROLE_NAME)
                                                           .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                                                                                     MANAGE_DEGREE))
                                                           .build();

    private static final RoleDto CURATOR_THESIS_EMBARGO_ROLE = RoleDto.newBuilder()
                                                                   .withRoleName(CURATOR_THESIS_EMBARGO_ROLE_NAME)
                                                                   .withAccessRights(
                                                                       List.of(MANAGE_RESOURCES_STANDARD,
                                                                               MANAGE_DEGREE_EMBARGO))
                                                                   .build();

    private static final RoleDto APPLICATION_ADMIN_ROLE = RoleDto.newBuilder()
                                                              .withRoleName(APP_ADMIN_ROLE_NAME)
                                                              .withAccessRights(List.of(MANAGE_CUSTOMERS,
                                                                                        MANAGE_EXTERNAL_CLIENTS,
                                                                                        ACT_AS,
                                                                                        MANAGE_IMPORT,
                                                                                        MANAGE_NVI))
                                                              .build();

    private static final RoleDto EDITOR_ROLE = RoleDto.newBuilder()
                                                   .withRoleName(EDITOR_ROLE_NAME)
                                                   .withAccessRights(List.of(MANAGE_OWN_AFFILIATION,
                                                                             MANAGE_RESOURCES_ALL))
                                                   .build();



    @Override
    public List<RoleDto> roles() {
        return List.of(CREATOR_ROLE,
                       PUBLISHING_CURATOR_ROLE,
                       NVI_CURATOR_ROLE,
                       DOI_CURATOR_ROLE,
                       SUPPORT_CURATOR_ROLE,
                       INSTITUTION_ADMIN_ROLE,
                       APPLICATION_ADMIN_ROLE,
                       EDITOR_ROLE,
                       INTERNAL_IMPORTER,
                       CURATOR_THESIS_ROLE,
                       CURATOR_THESIS_EMBARGO_ROLE);
    }
}
