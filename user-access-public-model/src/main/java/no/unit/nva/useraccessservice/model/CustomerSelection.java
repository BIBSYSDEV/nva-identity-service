package no.unit.nva.useraccessservice.model;

import no.unit.nva.identityservice.json.JsonConfig;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

import static nva.commons.core.attempt.Try.attempt;

public class CustomerSelection {

    public static final String TYPE_VALUE = "CustomerSelection";

    private URI customerId;

    public static CustomerSelection fromCustomerId(URI customerId) {
        var customerSelection = new CustomerSelection();
        customerSelection.setCustomerId(customerId);
        return customerSelection;
    }

    public static CustomerSelection fromJson(String body) {
        return attempt(() -> JsonConfig.readValue(body, CustomerSelection.class)).orElseThrow();
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @JacocoGenerated
    public String getType() {
        return TYPE_VALUE;
    }

    @JacocoGenerated
    public void setType(String type) {
        // DO nothing
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
