package no.unit.nva.handlers.models;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.model.RoleDto;
import nva.commons.core.JacocoGenerated;

public class CreateUserRequest {

    protected static final String NATIONAL_IDENTITY_NUMBER_FIELD = "nationalIdentityNumber";
    protected static final String CUSTOMER_ID_FIELD = "customerId";
    protected static final String ROLES_FIELD = "roles";

    @JsonProperty(NATIONAL_IDENTITY_NUMBER_FIELD)
    @JsonAlias("nin")
    private String nin;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(ROLES_FIELD)
    private Set<RoleDto> roles;

    public CreateUserRequest(String nin,
                             URI customerId,
                             Set<RoleDto> roles) {
        this.nin = nin;
        this.customerId = customerId;
        this.roles = roles;
    }

    @JacocoGenerated
    public CreateUserRequest() {

    }

    @JacocoGenerated
    public String getNin() {
        return nin;
    }

    @JacocoGenerated
    public void setNin(String nin) {
        this.nin = nin;
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
    public Set<RoleDto> getRoles() {
        return roles;
    }

    @JacocoGenerated
    public void setRoles(Set<RoleDto> roles) {
        this.roles = roles;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getNin(), getCustomerId(), getRoles());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateUserRequest)) {
            return false;
        }
        CreateUserRequest that = (CreateUserRequest) o;
        return Objects.equals(getNin(), that.getNin())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getRoles(), that.getRoles());
    }

    @Override
    public String toString() {
        return attempt(() -> JsonConfig.writeValueAsString(this)).orElseThrow();
    }
}
