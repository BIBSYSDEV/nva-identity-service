package no.unit.nva.handlers.data;

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
import java.util.List;
import no.unit.nva.handlers.RoleSource;
import no.unit.nva.useraccessservice.model.RoleDto;

public class DefaultRoleSource implements RoleSource {

    protected static final String CURATOR_ROLE_NAME = "Curator";
    protected static final String INSTITUTION_ADMIN_ROLE_NAME = "Institution-admin";
    protected static final String CURATOR_IMPORT_CANDIDATE_ROLE_NAME = "Curator-Import-candidate";
    protected static final String CURATOR_THESIS_ROLE_NAME = "Curator-thesis";
    protected static final String CURATOR_THESIS_EMBARGO_ROLE_NAME = "Curator-thesis-embargo";
    protected static final String APP_ADMIN_ROLE_NAME = "App-admin";
    protected static final String EDITOR_ROLE_NAME = "Editor";

    private static final RoleDto CREATOR_ROLE = RoleDto.newBuilder()
                                                    .withRoleName(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT)
                                                    .build();

    private static final RoleDto CURATOR_ROLE = RoleDto.newBuilder()
                                                    .withRoleName(CURATOR_ROLE_NAME)
                                                    .withAccessRights(List.of(APPROVE_DOI_REQUEST,
                                                                              REJECT_DOI_REQUEST,
                                                                              READ_DOI_REQUEST,
                                                                              EDIT_OWN_INSTITUTION_RESOURCES,
                                                                              APPROVE_PUBLISH_REQUEST))
                                                    .build();

    private static final RoleDto INSTITUTION_ADMIN_ROLE = RoleDto.newBuilder()
                                                              .withRoleName(INSTITUTION_ADMIN_ROLE_NAME)
                                                              .withAccessRights(List.of(EDIT_OWN_INSTITUTION_RESOURCES,
                                                                                        EDIT_OWN_INSTITUTION_PROJECTS,
                                                                                        EDIT_OWN_INSTITUTION_USERS))
                                                              .build();

    private static final RoleDto CURATOR_IMPORT_CANDIDATE_ROLE = RoleDto.newBuilder()
                                                                     .withRoleName(CURATOR_IMPORT_CANDIDATE_ROLE_NAME)
                                                                     .withAccessRights(
                                                                         List.of(PROCESS_IMPORT_CANDIDATE))
                                                                     .build();

    private static final RoleDto CURATOR_THESIS_ROLE = RoleDto.newBuilder()
                                                           .withRoleName(CURATOR_THESIS_ROLE_NAME)
                                                           .withAccessRights(List.of(PUBLISH_DEGREE))
                                                           .build();

    private static final RoleDto CURATOR_THESIS_EMBARGO_ROLE = RoleDto.newBuilder()
                                                                   .withRoleName(CURATOR_THESIS_EMBARGO_ROLE_NAME)
                                                                   .withAccessRights(
                                                                       List.of(PUBLISH_DEGREE_EMBARGO_READ))
                                                                   .build();

    private static final RoleDto APPLICATION_ADMIN_ROLE = RoleDto.newBuilder().withRoleName(APP_ADMIN_ROLE_NAME)
                                                              .withAccessRights(List.of(ADMINISTRATE_APPLICATION))
                                                              .build();

    private static final RoleDto EDITOR_ROLE = RoleDto.newBuilder().withRoleName(EDITOR_ROLE_NAME)
                                                   .withAccessRights(List.of(EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW,
                                                                             EDIT_ALL_NON_DEGREE_RESOURCES))
                                                   .build();

    @Override
    public List<RoleDto> roles() {
        return List.of(CREATOR_ROLE,
                       CURATOR_ROLE,
                       INSTITUTION_ADMIN_ROLE,
                       APPLICATION_ADMIN_ROLE,
                       EDITOR_ROLE,
                       CURATOR_IMPORT_CANDIDATE_ROLE,
                       CURATOR_THESIS_ROLE,
                       CURATOR_THESIS_EMBARGO_ROLE);
    }
}
