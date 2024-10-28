package no.unit.nva.useraccessservice.model;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.time.ZonedDateTime;

public record LicenseDto(
        ZonedDateTime signedDate,
        URI licenseUri
) implements JsonSerializable {

}

