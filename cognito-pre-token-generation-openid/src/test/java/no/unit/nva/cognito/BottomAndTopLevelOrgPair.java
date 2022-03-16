package no.unit.nva.cognito;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.cognito.cristin.org.CristinOrgResponse;

public class BottomAndTopLevelOrgPair {

    private final URI topLevelOrg;
    private final URI bottomLevelOrg;

    public BottomAndTopLevelOrgPair(URI bottomLevelOrg, URI topLevelOrg) {
        this.bottomLevelOrg = bottomLevelOrg;
        this.topLevelOrg = topLevelOrg;
    }

    public URI getTopLevelOrg() {
        return topLevelOrg;
    }

    public URI getBottomLevelOrg() {
        return bottomLevelOrg;
    }

    public CristinOrgResponse toCristinOrgResponse() {
        return CristinOrgResponse.create(bottomLevelOrg, topLevelOrg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTopLevelOrg(), getBottomLevelOrg());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BottomAndTopLevelOrgPair)) {
            return false;
        }
        BottomAndTopLevelOrgPair that = (BottomAndTopLevelOrgPair) o;
        return Objects.equals(getTopLevelOrg(), that.getTopLevelOrg()) && Objects.equals(
            getBottomLevelOrg(), that.getBottomLevelOrg());
    }
}
