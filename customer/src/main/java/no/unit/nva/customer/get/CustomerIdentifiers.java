package no.unit.nva.customer.get;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import no.unit.nva.customer.RestConfig;

public class CustomerIdentifiers {

    private final URI customerId;
    private final URI cristinId;

    public CustomerIdentifiers(URI customerId, URI cristinId) {
        this.customerId = customerId;
        this.cristinId = cristinId;
    }

    public URI getCustomerId() {
        return customerId;
    }

    public URI getCristinId() {
        return cristinId;
    }

    @Override
    public String toString() {
        return attempt(() -> RestConfig.defaultRestObjectMapper.asString(this)).orElseThrow();
    }
}
