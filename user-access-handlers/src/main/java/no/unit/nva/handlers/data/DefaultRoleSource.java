package no.unit.nva.handlers.data;

import no.unit.nva.handlers.RoleSource;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.model.RoleName;

import java.util.List;

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
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static nva.commons.apigateway.AccessRight.SUPPORT;

public class DefaultRoleSource implements RoleSource {

    public static final RoleDto PUBLISHING_CURATOR_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.PUBLISHING_CURATOR)
            .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                    MANAGE_PUBLISHING_REQUESTS,
                    MANAGE_RESOURCE_FILES))
            .build();
    public static final RoleDto NVI_CURATOR_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.NVI_CURATOR)
            .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                    MANAGE_NVI_CANDIDATES))
            .build();
    public static final RoleDto DOI_CURATOR_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.DOI_CURATOR)
            .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                    MANAGE_DOI))
            .build();
    public static final RoleDto SUPPORT_CURATOR_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.SUPPORT_CURATOR)
            .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                    SUPPORT))
            .build();
    private static final RoleDto CREATOR_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.CREATOR)
            .withAccessRights(List.of(MANAGE_OWN_RESOURCES))
            .build();
    private static final RoleDto INSTITUTION_ADMIN_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.INSTITUTION_ADMIN)
            .withAccessRights(List.of(MANAGE_OWN_AFFILIATION))
            .build();

    private static final RoleDto INTERNAL_IMPORTER = RoleDto.newBuilder()
            .withRoleName(RoleName.INTERNAL_IMPORTER)
            .withAccessRights(
                    List.of(MANAGE_IMPORT))
            .build();

    private static final RoleDto CURATOR_THESIS_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.THESIS_CURATOR)
            .withAccessRights(List.of(MANAGE_RESOURCES_STANDARD,
                    MANAGE_DEGREE))
            .build();

    private static final RoleDto CURATOR_THESIS_EMBARGO_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.EMBARGO_THESIS_CURATOR)
            .withAccessRights(
                    List.of(MANAGE_RESOURCES_STANDARD,
                            MANAGE_DEGREE_EMBARGO))
            .build();

    private static final RoleDto APPLICATION_ADMIN_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.APPLICATION_ADMIN)
            .withAccessRights(List.of(MANAGE_CUSTOMERS,
                    MANAGE_EXTERNAL_CLIENTS,
                    ACT_AS,
                    MANAGE_IMPORT,
                    MANAGE_NVI))
            .build();

    private static final RoleDto EDITOR_ROLE = RoleDto.newBuilder()
            .withRoleName(RoleName.EDITOR)
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
