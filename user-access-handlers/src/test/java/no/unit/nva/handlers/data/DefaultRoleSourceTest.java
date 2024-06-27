package no.unit.nva.handlers.data;

import static no.unit.nva.handlers.data.DefaultRoleSource.APP_ADMIN_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_THESIS_EMBARGO_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.CURATOR_THESIS_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.DOI_CURATOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.EDITOR_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.INSTITUTION_ADMIN_ROLE_NAME;
import static no.unit.nva.handlers.data.DefaultRoleSource.INTERNAL_IMPORTER_ROLE_NAME;
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
import no.unit.nva.useraccessservice.model.RoleName;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.Test;

public class DefaultRoleSourceTest {

    private final RoleSource roleSource;

    public DefaultRoleSourceTest() {
        this.roleSource = new DefaultRoleSource();
    }

    @Test
    void creatorsShouldHaveCorrectAccessRights() {
        var creatorRole = getRoleByName(RoleName.CREATOR);

        assertThat(creatorRole.getAccessRights(), containsInAnyOrder(MANAGE_OWN_RESOURCES));
    }

    @Test
    void doiCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(RoleName.DOI_CURATOR);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(MANAGE_DOI,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void supportCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(RoleName.SUPPORT_CURATOR);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(SUPPORT,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void publishingCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(RoleName.PUBLISHING_CURATOR);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(MANAGE_PUBLISHING_REQUESTS,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void nviCuratorsShouldHaveCorrectAccessRights() {
        var curatorRole = getRoleByName(RoleName.NVI_CURATOR);

        assertThat(curatorRole.getAccessRights(), containsInAnyOrder(MANAGE_NVI_CANDIDATES,
                                                                     MANAGE_RESOURCES_STANDARD));
    }

    @Test
    void importCandidateCuratorsShouldHaveCorrectAccessRights() {
        var importCandidateCuratorRole = getRoleByName(RoleName.INTERNAL_IMPORTER);

        assertThat(importCandidateCuratorRole.getAccessRights(), containsInAnyOrder(MANAGE_IMPORT));
    }

    @Test
    void thesisCuratorsShouldHaveCorrectAccessRights() {
        var thesisCuratorRole = getRoleByName(RoleName.THESIS_CURATOR);

        assertThat(thesisCuratorRole.getAccessRights(), containsInAnyOrder(MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE));
    }

    @Test
    void thesisEmbargoCuratorsShouldHaveCorrectAccessRights() {
        var thesisEmbargoCuratorRole = getRoleByName(RoleName.EMBARGO_THESIS_CURATOR);

        assertThat(thesisEmbargoCuratorRole.getAccessRights(), containsInAnyOrder(MANAGE_RESOURCES_STANDARD,
                                                                                  MANAGE_DEGREE_EMBARGO));
    }

    @Test
    void institutionAdminsShouldHaveCorrectAccessRights() {
        var institutionAdminRole = getRoleByName(RoleName.INSTITUTION_ADMIN);

        assertThat(institutionAdminRole.getAccessRights(), containsInAnyOrder(MANAGE_OWN_AFFILIATION));
    }

    @Test
    void appAdminsShouldHaveCorrectAccessRights() {
        var appAdminRole = getRoleByName(RoleName.APPLICATION_ADMIN);

        assertThat(appAdminRole.getAccessRights(), containsInAnyOrder(MANAGE_CUSTOMERS,
                                                                      MANAGE_EXTERNAL_CLIENTS,
                                                                      ACT_AS,
                                                                      MANAGE_NVI,
                                                                      MANAGE_IMPORT));
    }

    @Test
    void editorsShouldHaveCorrectAccessRights() {
        var editorRole = getRoleByName(RoleName.EDITOR);

        assertThat(editorRole.getAccessRights(), containsInAnyOrder(MANAGE_OWN_AFFILIATION,
                                                                    MANAGE_RESOURCES_ALL));
    }

    @Test
    void shouldReturnExpectedNumberOfRoles() {
        var expectedNumberOfRoles = List.of(RoleName.CREATOR,
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

    private RoleDto getRoleByName(RoleName roleName) {
        return roleSource.roles().stream()
                   .filter(role -> role.getRoleName().equals(roleName))
                   .collect(SingletonCollector.collect());
    }
}
