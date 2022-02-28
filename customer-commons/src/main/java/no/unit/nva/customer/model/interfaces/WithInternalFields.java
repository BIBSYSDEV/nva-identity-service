package no.unit.nva.customer.model.interfaces;

import java.time.Instant;
import java.util.UUID;

public interface WithInternalFields {

    UUID getIdentifier();

    void setIdentifier(UUID identifier);

    Instant getCreatedDate();

    void setCreatedDate(Instant createdDate);

    Instant getModifiedDate();

    void setModifiedDate(Instant modifiedDate);

}
