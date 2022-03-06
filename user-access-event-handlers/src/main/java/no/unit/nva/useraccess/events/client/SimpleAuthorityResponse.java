package no.unit.nva.useraccess.events.client;

import static java.util.Objects.nonNull;
import static no.unit.nva.identityservice.json.JsonConfig.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;

public class SimpleAuthorityResponse {

    private URI id;
    @JsonProperty("orgunitids")
    private List<URI> organizationIds;

    public SimpleAuthorityResponse() {
    }

    public static SimpleAuthorityResponse fromJson(String json) {
        return attempt(() -> objectMapper.beanFrom(SimpleAuthorityResponse.class, json)).orElseThrow();
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

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getOrganizationIds());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SimpleAuthorityResponse)) {
            return false;
        }
        SimpleAuthorityResponse that = (SimpleAuthorityResponse) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getOrganizationIds(),
                                                                       that.getOrganizationIds());
    }

    @Override
    public String toString() {
        return attempt(() -> objectMapper.asString(this)).orElseThrow();
    }
}
