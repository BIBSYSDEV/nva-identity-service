package no.unit.nva.handlers.models;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.identityservice.json.JsonConfig;
import no.unit.nva.useraccessservice.model.RoleDto;
import no.unit.nva.useraccessservice.usercreation.cristin.NationalIdentityNumber;
import nva.commons.core.JacocoGenerated;

public class CreateUserRequest {

    @JsonProperty("nationalIdentityNumber")
    @JsonAlias("nin")
    private NationalIdentityNumber nin;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("roles")
    private List<RoleDto> roles;

    public CreateUserRequest(NationalIdentityNumber nin,
                             URI customerId,
                             List<RoleDto> roles) {
        this.nin = nin;
        this.customerId = customerId;
        this.roles = roles;
    }

    @JacocoGenerated
    public CreateUserRequest() {

    }

    public static CreateUserRequest fromJson(String json) {
        return attempt(() -> JsonConfig.readValue(json, CreateUserRequest.class)).orElseThrow();
    }

    @JacocoGenerated
    public NationalIdentityNumber getNin() {
        return nin;
    }

    @JacocoGenerated
    public void setNin(NationalIdentityNumber nin) {
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
    public List<RoleDto> getRoles() {
        return roles;
    }

    @JacocoGenerated
    public void setRoles(List<RoleDto> roles) {
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
