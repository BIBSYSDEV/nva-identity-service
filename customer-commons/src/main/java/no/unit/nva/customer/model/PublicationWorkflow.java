package no.unit.nva.customer.model;

public enum PublicationWorkflow {
    REGISTRATOR_PUBLISHES_METADATA_ONLY("RegistratorPublishesMetadataOnly"),
    REGISTRATOR_PUBLISHES_METADATA_AND_FILES("RegistratorPublishesMetadataAndFiles");

    private final String value;

    PublicationWorkflow(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
