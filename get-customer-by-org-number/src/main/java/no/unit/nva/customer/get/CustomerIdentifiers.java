package no.unit.nva.customer.get;

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
}
