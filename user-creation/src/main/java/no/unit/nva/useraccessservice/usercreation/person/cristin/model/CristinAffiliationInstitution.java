package no.unit.nva.useraccessservice.usercreation.person.cristin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CristinAffiliationInstitution {
    @JsonProperty("cristin_institution_id")
    private final String id;
    @JsonProperty("url")
    private final String url;

    public CristinAffiliationInstitution(@JsonProperty("cristin_institution_id") String id,
                                         @JsonProperty("url") String url) {
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
