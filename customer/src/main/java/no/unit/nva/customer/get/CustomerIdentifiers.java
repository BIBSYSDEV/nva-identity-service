package no.unit.nva.customer.get;

import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;

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
        return attempt(() -> objectMapper.asString(this)).orElseThrow();
    }
}
