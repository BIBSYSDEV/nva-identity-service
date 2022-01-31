package no.unit.nva.useraccess.events.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JsonUtils;
import nva.commons.core.paths.UnixPath;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.nonNull;
import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleAuthorityResponse {

    private URI id;
    @JsonProperty("orgunitids")
    private List<URI> organizationIds;

    public SimpleAuthorityResponse() {
    }

    public static SimpleAuthorityResponse fromJson(String body) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(body, SimpleAuthorityResponse.class)).orElseThrow();
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getSystemControlNumber() {
        return UnixPath.of(id.toString()).getFilename();
    }

    public List<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : Collections.emptyList();
    }

    public void setOrganizationIds(List<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }

    public String toJson() {
        return attempt(() -> dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
