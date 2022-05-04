package no.unit.nva.useraccessservice.usercreation.cristin.org;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.Test;

class CristinOrgResponseTest {

    @Test
    void shouldReturnOrgStructureWithExpectedParentsAsOrdered() {
        var orgUri = randomUri();
        var parentOrgUri = randomUri();
        var grandParentOrgUri = randomUri();
        var institutionUri = randomUri();

        var organization = CristinOrgResponse.create(orgUri, parentOrgUri, grandParentOrgUri, institutionUri);
        assertThat(organization.getOrgId(), is(equalTo(orgUri)));

        var parentOrg = extractParent(organization);
        assertThat(parentOrg.getOrgId(), is(equalTo(parentOrgUri)));

        var grandParentOrg = extractParent(parentOrg);
        assertThat(grandParentOrg.getOrgId(), is(equalTo(grandParentOrgUri)));

        assertThat(organization.extractInstitutionUri(), is(equalTo(institutionUri)));
    }

    private CristinOrgResponse extractParent(CristinOrgResponse organization) {
        return organization.getPartOf().stream().collect(SingletonCollector.collect());
    }
}