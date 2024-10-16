package no.unit.nva.useraccessservice.model;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.time.LocalDateTime;

public record LicenseDto(
        LocalDateTime signedDate,
        URI licenseUri
) implements JsonSerializable {


    public LicenseDto() {
        this(null, null);
    }
}

