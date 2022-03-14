package no.unit.nva.cognito;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.cognito.cristin.org.CristinOrgResponse;
import no.unit.nva.cognito.cristin.person.CristinAffiliation;
import no.unit.nva.cognito.cristin.person.CristinPersonResponse;

public class TopLevelOrg {

    private final URI topLevelOrg;
    private final URI extractedFromAffiliation;
    private final boolean isActiveAffiliation;

    public TopLevelOrg(URI topLevelOrg, URI extractedFromAffiliation, boolean isActiveAffiliation) {
        this.topLevelOrg = topLevelOrg;
        this.extractedFromAffiliation = extractedFromAffiliation;
        this.isActiveAffiliation = isActiveAffiliation;
    }

    public static TopLevelOrg create(CristinOrgResponse cristinOrgResponse,
                                     CristinPersonResponse cristinPersonResponse) {
        var affiliationUri= cristinOrgResponse.getOrgId();
        var isAffiliationActive = investigateIfAffiliationIsActive(cristinPersonResponse, affiliationUri);
        return new TopLevelOrg(cristinOrgResponse.extractTopOrgUri(),URI.create(affiliationUri),isAffiliationActive);
    }

    private static boolean investigateIfAffiliationIsActive(CristinPersonResponse cristinPersonResponse, String affiliationUri) {
        return cristinPersonResponse.getAffiliations()
            .stream()
            .filter(aff->aff.getOrganization().equals(affiliationUri))
            .anyMatch(CristinAffiliation::isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTopLevelOrg(), getExtractedFromAffiliation(), isActiveAffiliation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopLevelOrg)) {
            return false;
        }
        TopLevelOrg that = (TopLevelOrg) o;
        return isActiveAffiliation() == that.isActiveAffiliation()
               && Objects.equals(getTopLevelOrg(), that.getTopLevelOrg())
               && Objects.equals(getExtractedFromAffiliation(), that.getExtractedFromAffiliation());
    }

    public URI getTopLevelOrg() {
        return topLevelOrg;
    }

    public URI getExtractedFromAffiliation() {
        return extractedFromAffiliation;
    }

    public boolean isActiveAffiliation() {
        return isActiveAffiliation;
    }
}
