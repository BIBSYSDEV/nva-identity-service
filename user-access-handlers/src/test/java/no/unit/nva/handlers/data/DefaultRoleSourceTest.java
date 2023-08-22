package no.unit.nva.handlers.data;

import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static no.unit.nva.handlers.data.DefaultRoleSource.APP_ADMIN_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_IMPORT_CANDIDATE_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_THESIS_EMBARGO_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_THESIS_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.EDITOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.INSTITUTION_ADMIN_ROLE_NAME;
import static nva.commons.apigateway.AccessRight.ADMINISTRATE_APPLICATION;
import static nva.commons.apigateway.AccessRight.APPROVE_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.APPROVE_PUBLISH_REQUEST;
import static nva.commons.apigateway.AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_USERS;
import static nva.commons.apigateway.AccessRight.MANAGE_NVI_PERIODS;
import static nva.commons.apigateway.AccessRight.MANAGE_OWN_PROJECTS;
import static nva.commons.apigateway.AccessRight.PROCESS_IMPORT_CANDIDATE;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE_EMBARGO_READ;
import static nva.commons.apigateway.AccessRight.READ_DOI_REQUEST;
import static nva.commons.apigateway.AccessRight.REJECT_DOI_REQUEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import java.util.List;
import no.unit.nva.handlers.RoleSource;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.Test;

public class DefaultRoleSourceTest {

    private final RoleSource roleSource;

    public DefaultRoleSourceTest() {
        this.roleSource = new DefaultRoleSource();
    }

    @Test
    void creatorsShouldHaveCorrectAccessRights() {
        var creatorRole = getRoleByName(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT);

        assertThat(creatorRole.getAccessRights(), containsInAnyOrder(MANAGE_OWN_PROJECTS));
    }

    @Test
    void curatorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(CURATOR_ROLE_NAME);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(APPROVE_DOI_REQUEST,
                                                                     REJECT_DOI_REQUEST,
                                                                     READ_DOI_REQUEST,
                                                                     EDIT_OWN_INSTITUTION_RESOURCES,
                                                                     APPROVE_PUBLISH_REQUEST));
    }

    @Test
    void importCandidateCuratorsShouldHaveCorrectAccessRights() {
        var importCandidateCuratorRole = getRoleByName(CURATOR_IMPORT_CANDIDATE_ROLE_NAME);

        assertThat(importCandidateCuratorRole.getAccessRights(), containsInAnyOrder(PROCESS_IMPORT_CANDIDATE));
    }

    @Test
    void thesisCuratorsShouldHaveCorrectAccessRights() {
        var thesisCuratorRole = getRoleByName(CURATOR_THESIS_ROLE_NAME);

        assertThat(thesisCuratorRole.getAccessRights(), containsInAnyOrder(PUBLISH_DEGREE));
    }

    @Test
    void thesisEmbargoCuratorsShouldHaveCorrectAccessRights() {
        var thesisEmbargoCuratorRole = getRoleByName(CURATOR_THESIS_EMBARGO_ROLE_NAME);

        assertThat(thesisEmbargoCuratorRole.getAccessRights(), containsInAnyOrder(PUBLISH_DEGREE_EMBARGO_READ));
    }

    @Test
    void institutionAdminsShouldHaveCorrectAccessRights() {
        var institutionAdminRole = getRoleByName(INSTITUTION_ADMIN_ROLE_NAME);

        assertThat(institutionAdminRole.getAccessRights(), containsInAnyOrder(EDIT_OWN_INSTITUTION_RESOURCES,
                                                                              EDIT_OWN_INSTITUTION_USERS));
    }

    @Test
    void appAdminsShouldHaveCorrectAccessRights() {
        var appAdminRole = getRoleByName(APP_ADMIN_ROLE_NAME);

        assertThat(appAdminRole.getAccessRights(), containsInAnyOrder(ADMINISTRATE_APPLICATION,
                                                                      PROCESS_IMPORT_CANDIDATE,
                                                                      MANAGE_NVI_PERIODS));
    }

    @Test
    void editorsShouldHaveCorrectAccessRights() {
        var editorRole = getRoleByName(EDITOR_ROLE_NAME);

        assertThat(editorRole.getAccessRights(), containsInAnyOrder(EDIT_OWN_INSTITUTION_PUBLICATION_WORKFLOW,
                                                                    EDIT_ALL_NON_DEGREE_RESOURCES));
    }

    @Test
    void shouldReturnExpectedNumberOfRoles() {
        var expectedNumberOfRoles = List.of(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT,
                                            CURATOR_ROLE_NAME,
                                            CURATOR_THESIS_ROLE_NAME,
                                            CURATOR_THESIS_EMBARGO_ROLE_NAME,
                                            CURATOR_IMPORT_CANDIDATE_ROLE_NAME,
                                            INSTITUTION_ADMIN_ROLE_NAME,
                                            APP_ADMIN_ROLE_NAME,
                                            EDITOR_ROLE_NAME).size();

        assertThat(roleSource.roles(), hasSize(expectedNumberOfRoles));
    }

    private RoleDto getRoleByName(String roleName) {
        return roleSource.roles().stream()
                   .filter(role -> roleName.equals(role.getRoleName()))
                   .collect(SingletonCollector.collect());
    }
}
