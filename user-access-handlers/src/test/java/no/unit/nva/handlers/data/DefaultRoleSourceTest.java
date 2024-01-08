package no.unit.nva.handlers.data;

import static no.unit.nva.database.IdentityService.Constants.ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT;
import static no.unit.nva.handlers.data.DefaultRoleSource.APP_ADMIN_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.DOI_CURATOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.INTERNAL_IMPORTER_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_THESIS_EMBARGO_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_THESIS_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.EDITOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.INSTITUTION_ADMIN_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.NVI_CURATOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.PUBLISHING_CURATOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.SUPPORT_CURATOR_ROLE_NAME;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
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

        assertThat(creatorRole.getAccessRights(), containsInAnyOrder(MANAGE_OWN_RESOURCES));
    }

    @Test
    void doiCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(DOI_CURATOR_ROLE_NAME);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(MANAGE_DOI,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void supportCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(SUPPORT_CURATOR_ROLE_NAME);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(SUPPORT,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void publishingCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(PUBLISHING_CURATOR_ROLE_NAME);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(MANAGE_PUBLISHING_REQUESTS,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void nviCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(NVI_CURATOR_ROLE_NAME);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(MANAGE_NVI_CANDIDATES,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void importCandidateCuratorsShouldHaveCorrectAccessRights() {
        var importCandidateCuratorRole = getRoleByName(INTERNAL_IMPORTER_ROLE_NAME);

        assertThat(importCandidateCuratorRole.getAccessRights(), containsInAnyOrder(MANAGE_IMPORT));
    }

    @Test
    void thesisCuratorsShouldHaveCorrectAccessRights() {
        var thesisCuratorRole = getRoleByName(CURATOR_THESIS_ROLE_NAME);

        assertThat(thesisCuratorRole.getAccessRights(), containsInAnyOrder(MANAGE_DEGREE));
    }

    @Test
    void thesisEmbargoCuratorsShouldHaveCorrectAccessRights() {
        var thesisEmbargoCuratorRole = getRoleByName(CURATOR_THESIS_EMBARGO_ROLE_NAME);

        assertThat(thesisEmbargoCuratorRole.getAccessRights(), containsInAnyOrder(MANAGE_DEGREE_EMBARGO));
    }

    @Test
    void institutionAdminsShouldHaveCorrectAccessRights() {
        var institutionAdminRole = getRoleByName(INSTITUTION_ADMIN_ROLE_NAME);

        assertThat(institutionAdminRole.getAccessRights(), containsInAnyOrder(MANAGE_RESOURCES_STANDARD,
                                                                              MANAGE_OWN_AFFILIATION));
    }

    @Test
    void appAdminsShouldHaveCorrectAccessRights() {
        var appAdminRole = getRoleByName(APP_ADMIN_ROLE_NAME);

        assertThat(appAdminRole.getAccessRights(), containsInAnyOrder(MANAGE_CUSTOMERS,
                                                                      MANAGE_EXTERNAL_CLIENTS,
                                                                      ACT_AS,
                                                                      MANAGE_NVI,
                                                                      MANAGE_IMPORT));
    }

    @Test
    void editorsShouldHaveCorrectAccessRights() {
        var editorRole = getRoleByName(EDITOR_ROLE_NAME);

        assertThat(editorRole.getAccessRights(), containsInAnyOrder(MANAGE_OWN_AFFILIATION,
                                                                    MANAGE_RESOURCES_ALL));
    }

    @Test
    void shouldReturnExpectedNumberOfRoles() {
        var expectedNumberOfRoles = List.of(ROLE_ACQUIRED_BY_ALL_PEOPLE_WITH_ACTIVE_EMPLOYMENT,
                                            NVI_CURATOR_ROLE_NAME,
                                            DOI_CURATOR_ROLE_NAME,
                                            SUPPORT_CURATOR_ROLE_NAME,
                                            PUBLISHING_CURATOR_ROLE_NAME,
                                            CURATOR_THESIS_ROLE_NAME,
                                            CURATOR_THESIS_EMBARGO_ROLE_NAME,
                                            INTERNAL_IMPORTER_ROLE_NAME,
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
