package no.unit.nva.cognito.cristin;

import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class BottomOrgTopOrgPair {

    private final URI bottomOrg;
    private final URI topOrg;

    public BottomOrgTopOrgPair(URI bottomOrg, URI topOrg) {
        this.bottomOrg = bottomOrg;
        this.topOrg = topOrg;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getBottomOrg(), getTopOrg());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BottomOrgTopOrgPair)) {
            return false;
        }
        BottomOrgTopOrgPair that = (BottomOrgTopOrgPair) o;
        return Objects.equals(getBottomOrg(), that.getBottomOrg()) && Objects.equals(getTopOrg(),
                                                                                     that.getTopOrg());
    }

    public URI getBottomOrg() {
        return bottomOrg;
    }

    public URI getTopOrg() {
        return topOrg;
    }
}
