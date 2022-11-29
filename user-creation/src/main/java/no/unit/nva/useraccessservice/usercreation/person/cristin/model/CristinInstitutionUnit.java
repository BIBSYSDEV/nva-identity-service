package no.unit.nva.useraccessservice.usercreation.person.cristin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CristinInstitutionUnit {
    @JsonProperty("cristin_unit_id")
    private final String id;
    @JsonProperty("url")
    private final String url;

    @JsonCreator
    public CristinInstitutionUnit(@JsonProperty("cristin_unit_id") String id, @JsonProperty("url") String url) {
        this.id = id;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }
}
